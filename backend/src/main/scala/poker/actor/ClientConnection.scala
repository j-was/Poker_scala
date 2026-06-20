package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.*
import poker.protocol.ServerMessage

import scala.concurrent.duration._
import org.apache.pekko.util.Timeout

implicit val timeout: Timeout = Timeout(3.seconds)


/**
 * One actor per active WebSocket connection.
 *
 * Lifecycle:
 *  1. Spawned by WebSocketServer on each new WS connection.
 *     2. Waits for Identify; registers its outgoing channel with SessionRegistry.
 *     3. Routes all game commands to the appropriate GameInstance actor.
 *     4. On ConnectionClosed: broadcasts PlayerDisconnected, unregisters from
 *     SessionRegistry. The player's seat in GameInstance is preserved for reconnect.
 *     5. A reconnect with the same playerId creates a NEW ClientConnection that
 *     re-registers its outgoing channel, overwriting the stale one.
 */
object ClientConnection {

  sealed trait Command

  case class IncomingMessage(msg: poker.protocol.ClientMessage) extends Command

  case object ConnectionClosed extends Command

  private case class OnGameCreated(code: String, ref: ActorRef[GameInstance.Command]) extends Command

  private case class OnLookupResult(result: GameRegistry.LookupResult) extends Command

  private case class OnJoinResult(code: String, ref: ActorRef[GameInstance.Command],
                                  result: GameInstance.Response) extends Command

  private case class OnCreateJoinResult(code: String, ref: ActorRef[GameInstance.Command],
                                        result: GameInstance.Response) extends Command

  private case class OnActionResult(code: String, result: GameInstance.Response) extends Command

  private case class OnLeaveResult(code: String, result: GameInstance.Response) extends Command

  private case class OnFailure(reason: String) extends Command

  private case class OnRestoreGame(result: SessionRegistry.JoinedGameResult) extends Command
  private case class OnRestoreLookup(playerId: String, name: String,
                                     code: String, result: GameRegistry.LookupResult) extends Command
  private case class OnRestoredState(playerId: String, name: String,
                                     code: String, gameRef: ActorRef[GameInstance.Command], state: GameState) extends Command

  def apply(
             sessionRegistry: ActorRef[SessionRegistry.Command],
             gameRegistry: ActorRef[GameRegistry.Command],
             outgoing: ActorRef[ServerMessage]
           ): Behavior[Command] =
    waitingForIdentify(sessionRegistry, gameRegistry, outgoing)


  private def waitingForIdentify(
                                  sessionRegistry: ActorRef[SessionRegistry.Command],
                                  gameRegistry: ActorRef[GameRegistry.Command],
                                  outgoing: ActorRef[ServerMessage]
                                ): Behavior[Command] = Behaviors.setup {
    ctx => Behaviors.receiveMessage {
      case IncomingMessage(poker.protocol.ClientMessage.Identify(name, existingId)) =>
        val playerId = existingId.getOrElse(java.util.UUID.randomUUID().toString)
        sessionRegistry ! SessionRegistry.Register(playerId, outgoing)
        outgoing ! ServerMessage.Identified(playerId, name)

        ctx.ask(sessionRegistry, (r: ActorRef[SessionRegistry.JoinedGameResult]) =>
          SessionRegistry.GetJoinedGame(playerId, r)) {
          case scala.util.Success(result) => OnRestoreGame(result)
          case _ => OnFailure("Failed to reload previous session")
        }

        restoring(playerId, name, sessionRegistry, gameRegistry, outgoing)

      case ConnectionClosed =>
        Behaviors.stopped
      case _ =>
        outgoing ! ServerMessage.Error("First message must be Identify")
        Behaviors.same
    }
  }

  private def connected(
                         playerId: String,
                         name: String,
                         currentGame: Option[(String, ActorRef[GameInstance.Command])],
                         sessionRegistry: ActorRef[SessionRegistry.Command],
                         gameRegistry: ActorRef[GameRegistry.Command],
                         outgoing: ActorRef[ServerMessage]
                       ): Behavior[Command] = {
    Behaviors.setup { ctx =>

      Behaviors.receiveMessage {
        case IncomingMessage(poker.protocol.ClientMessage.Ping) =>
          outgoing ! ServerMessage.Pong
          Behaviors.same

        case IncomingMessage(poker.protocol.ClientMessage.CreateGame(settings)) =>
          ctx.ask(
            gameRegistry,
            (r: ActorRef[GameRegistry.CreateResult]) => GameRegistry.CreateGame(playerId, settings, r)
          ) {
            case scala.util.Success(GameRegistry.GameCreated(code, ref)) => OnGameCreated(code, ref)
            case _ => OnFailure("Failed to create game")
          }
          Behaviors.same

        case OnGameCreated(code, gameRef) =>
          ctx.ask(
            gameRef,
            (r: ActorRef[GameInstance.Response]) => GameInstance.JoinGame(playerId, name, r)
          ) {
            case scala.util.Success(res) => OnCreateJoinResult(code, gameRef, res)
            case _ => OnFailure("Failed to join the created game")
          }
          Behaviors.same

        case OnCreateJoinResult(code, gameRef, GameInstance.GameJoined(_, state)) =>
          sessionRegistry ! SessionRegistry.JoinedGame(playerId, code)
          outgoing ! ServerMessage.GameCreated(code, state.toClientView(playerId))
          connected(playerId, name, Some(code -> gameRef), sessionRegistry, gameRegistry, outgoing)

        case OnCreateJoinResult(_, _, GameInstance.Error(msg)) =>
          outgoing ! ServerMessage.Error(msg)
          Behaviors.same

        case IncomingMessage(poker.protocol.ClientMessage.JoinGame(code)) => {
          val normCode = code.trim.toLowerCase
          ctx.ask(
            gameRegistry,
            (r: ActorRef[GameRegistry.LookupResult]) => GameRegistry.LookupGame(normCode, r)
          ) {
            case scala.util.Success(res) => OnLookupResult(res)
            case _ => OnFailure(s"Lookup failed for '$normCode'")
          }
          Behaviors.same
        }

        case OnLookupResult(GameRegistry.Found(code, gameRef)) => {
          ctx.ask(
            gameRef,
            (r: ActorRef[GameInstance.Response]) => GameInstance.JoinGame(playerId, name, r)
          ) {
            case scala.util.Success(res) => OnJoinResult(code, gameRef, res)
            case _ => OnFailure("Join request timed out")
          }
          Behaviors.same
        }

        case OnLookupResult(GameRegistry.NotFound) => {
          outgoing ! ServerMessage.Error("Game not found. Check the code and try again.")
          Behaviors.same
        }

        case OnJoinResult(code, gameRef, GameInstance.GameJoined(_, state)) => {
          sessionRegistry ! SessionRegistry.JoinedGame(playerId, code)
          sessionRegistry ! SessionRegistry.BroadcastRawExcept(code, playerId, ServerMessage.PlayerJoined(code, playerId, name))
          outgoing ! ServerMessage.GameJoined(code, state.toClientView(playerId))
          sessionRegistry ! SessionRegistry.BroadcastToGame(code, state, (_, cs) => ServerMessage.StateUpdate(code, cs))
          connected(playerId, name, Some(code -> gameRef), sessionRegistry, gameRegistry, outgoing)
        }

        case OnJoinResult(_, _, GameInstance.Error(msg)) => {
          outgoing ! ServerMessage.Error(msg)
          Behaviors.same
        }

        case IncomingMessage(action) => {
          currentGame match
            case None =>
              outgoing ! ServerMessage.Error("You are not in a game")
              Behaviors.same
            case Some((code, gameRef)) =>
              dispatchAction(ctx, playerId, code, gameRef, action)
              Behaviors.same
        }

        // ── Action outcomes ─────────────────────────────────────────────────────
        case OnActionResult(code, GameInstance.ActionSuccess(state)) => {
          sessionRegistry ! SessionRegistry.BroadcastToGame(code, state,
            (_, cs) => ServerMessage.StateUpdate(code, cs))
          Behaviors.same
        }

        case OnActionResult(code, GameInstance.GameStarted(state)) => {
          sessionRegistry ! SessionRegistry.BroadcastToGame(code, state,
            (_, cs) => ServerMessage.GameStarted(code, cs))
          Behaviors.same
        }

        case OnActionResult(code, GameInstance.GameOver(winnerId, state)) => {
          val winnerName = state.players.find(_.id == winnerId).map(_.name).getOrElse("Unknown")
          sessionRegistry ! SessionRegistry.BroadcastToGame(code, state,
            (_, cs) => ServerMessage.GameOver(code, winnerId, winnerName, cs))
          Behaviors.same
        }

        case OnActionResult(_, GameInstance.Error(msg)) => {
          outgoing ! ServerMessage.Error(msg)
          Behaviors.same
        }

        case OnLeaveResult(code, GameInstance.ActionSuccess(state)) => {
          sessionRegistry ! SessionRegistry.LeftGame(playerId, code)
          sessionRegistry ! SessionRegistry.BroadcastToGame(code, state, (_, cs) => ServerMessage.StateUpdate(code, cs))
          sessionRegistry ! SessionRegistry.BroadcastRaw(code, ServerMessage.PlayerLeft(code, playerId, name))
          outgoing ! ServerMessage.StateUpdate(code, state.toClientView(playerId))
          connected(playerId, name, None, sessionRegistry, gameRegistry, outgoing)
        }

        case OnLeaveResult(_, GameInstance.Error(msg)) => {
          outgoing ! ServerMessage.Error(msg)
          Behaviors.same
        }

        case OnFailure(reason) => {
          outgoing ! ServerMessage.Error(reason)
          Behaviors.same
        }

        case ConnectionClosed => {
          currentGame.foreach { (code, _) =>
            sessionRegistry ! SessionRegistry.BroadcastRaw(code,
              ServerMessage.PlayerDisconnected(code, playerId, name))
          }
          // Unregister the outgoing channel only. GameInstance seat is preserved.
          sessionRegistry ! SessionRegistry.Unregister(playerId)
          Behaviors.stopped
        }

        case _ =>
          Behaviors.same
      }
    }
  }

  private def restoring(
                         playerId: String,
                         name: String,
                         sessionRegistry: ActorRef[SessionRegistry.Command],
                         gameRegistry: ActorRef[GameRegistry.Command],
                         outgoing: ActorRef[ServerMessage]
                       ): Behavior[Command] = {
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case OnRestoreGame(SessionRegistry.JoinedGameResult(None)) =>
          connected(playerId, name, None, sessionRegistry, gameRegistry, outgoing)
        case OnRestoreGame(SessionRegistry.JoinedGameResult(Some(code))) =>
          ctx.ask(gameRegistry, (r: ActorRef[GameRegistry.LookupResult]) =>
            GameRegistry.LookupGame(code, r)
          ) {
            case scala.util.Success(result) => OnRestoreLookup(playerId, name, code, result)
            case _ => OnFailure(s"Could not restore game '$code'")
          }
          Behaviors.same

        case OnRestoreLookup(_, _, code, GameRegistry.Found(_, gameRef)) =>
          ctx.ask(gameRef, (r: ActorRef[GameState]) => GameInstance.GetState(r)) {
            case scala.util.Success(state) => OnRestoredState(playerId, name, code, gameRef,
              state)
            case _ => OnFailure(s"Could not restore state for '$code'")
          }
          Behaviors.same

        case OnRestoreLookup(_, _, _, GameRegistry.NotFound) =>
          connected(playerId, name, None, sessionRegistry, gameRegistry, outgoing)

        case OnRestoredState(_, _, code, gameRef, state) =>
          outgoing ! ServerMessage.GameJoined(code, state.toClientView(playerId))
          sessionRegistry ! SessionRegistry.BroadcastRaw(code, ServerMessage.PlayerReconnected(code, playerId, name))
          connected(playerId, name, Some(code -> gameRef), sessionRegistry, gameRegistry, outgoing)

        case OnFailure(reason) =>
          outgoing ! ServerMessage.Error(reason)
          connected(playerId, name, None, sessionRegistry, gameRegistry, outgoing)

        case ConnectionClosed =>
          sessionRegistry ! SessionRegistry.Unregister(playerId)
          Behaviors.stopped

        case IncomingMessage(poker.protocol.ClientMessage.Ping) =>
          outgoing ! ServerMessage.Pong
          Behaviors.same

        case IncomingMessage(_) =>
          outgoing ! ServerMessage.Error("Session restore is still in progress")
          Behaviors.same
      }
      }
  }

  private def dispatchAction(
                              ctx: org.apache.pekko.actor.typed.scaladsl.ActorContext[Command],
                              playerId: String,
                              code: String,
                              gameRef: ActorRef[GameInstance.Command],
                              msg: poker.protocol.ClientMessage
                            ): Unit = {
    import poker.protocol.ClientMessage.*

    def ask(cmd: ActorRef[GameInstance.Response] => GameInstance.Command): Unit = {
      ctx.ask(gameRef, cmd) {
        case scala.util.Success(r) => OnActionResult(code, r)
        case _ => OnFailure("Game action failed or timed out")
      }
    }

    msg match {
      case StartGame(_) => ask(GameInstance.StartGame.apply)
      case Fold(_) => ask(GameInstance.Fold(playerId, _))
      case Call(_) => ask(GameInstance.Call(playerId, _))
      case Check(_) => ask(GameInstance.Check(playerId, _))
      case Raise(_, amount) => ask(GameInstance.Raise(playerId, amount, _))
      case LeaveGame(_) =>
        ctx.ask(
          gameRef,
          (r: ActorRef[GameInstance.Response]) => GameInstance.LeaveGame(playerId, r)
        ) {
          case scala.util.Success(res) => OnLeaveResult(code, res)
          case _ => OnFailure("Leave failed or timed out")
        }
      case _ => ()
    }
  }
}
