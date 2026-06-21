package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.*

/**
 * Actor wrapping one poker game (one "table").
 *
 * This is the actor shell; all game logic lives in poker.domain.PokerEngine.
 *
 * AutoFoldService integration (optional):
 * After each turn advance, notifies AutoFoldService so disconnected players
 * are automatically folded after a grace period.
 */
object GameInstance:

  sealed trait Command

  case class JoinGame(playerId: String, name: String, replyTo: ActorRef[Response]) extends Command

  case class StartGame(replyTo: ActorRef[Response]) extends Command

  case class Fold(playerId: String, replyTo: ActorRef[Response]) extends Command

  case class Call(playerId: String, replyTo: ActorRef[Response]) extends Command

  case class Check(playerId: String, replyTo: ActorRef[Response]) extends Command

  case class Raise(playerId: String, amount: Int, replyTo: ActorRef[Response]) extends Command

  case class LeaveGame(playerId: String, replyTo: ActorRef[Response]) extends Command

  case class GetState(replyTo: ActorRef[GameState]) extends Command

  case class UpdateSettings(settings: GameSettings, replyTo: ActorRef[Response]) extends Command

  case class UpdatePlayerCount(delta: Int) extends Command

  sealed trait Response

  case class GameJoined(playerId: String, state: GameState) extends Response

  case class GameStarted(state: GameState) extends Response

  case class ActionSuccess(state: GameState) extends Response

  case class GameOver(winnerId: String, state: GameState) extends Response

  case class Error(msg: String) extends Response

  case class SettingsUpdated(state: GameState) extends Response

  def apply(
             id: String,
             settings: GameSettings = GameSettings(),
             autoFoldService: Option[ActorRef[AutoFoldService.Command]] = None,
             sessionRegistry: Option[ActorRef[SessionRegistry.Command]] = None
           ): Behavior[Command] =
    Behaviors.setup { ctx =>
      waitingForPlayers(
        GameState(id = id, settings = settings),
        autoFoldService,
        sessionRegistry,
        ctx.self
      )
    }

  // ── Internal helper ────────────────────────────────────────────────────────

  private def notifyAutoFold(
                              gameId: String,
                              state: GameState,
                              autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                              sessionRegistry: Option[ActorRef[SessionRegistry.Command]],
                              self: ActorRef[Command]
                            ): Unit =
    for
      svc <- autoFoldService
      sessions <- sessionRegistry
      player <- state.currentPlayer
    do
      svc ! AutoFoldService.TurnAdvanced(gameId, player.id, self, sessions)

  // ── Phase: waiting for players ─────────────────────────────────────────────

  private def waitingForPlayers(
                                 state: GameState,
                                 autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                                 sessionRegistry: Option[ActorRef[SessionRegistry.Command]],
                                 self: ActorRef[Command]
                               ): Behavior[Command] = Behaviors.receiveMessage {

    case JoinGame(playerId, name, replyTo) =>
      if state.players.exists(_.id == playerId) then
        replyTo ! Error("Player already joined")
        Behaviors.same
      else
        val newPlayer = Player(playerId, name, state.settings.initialChips)
        val newState = state.copy(players = state.players :+ newPlayer)
        replyTo ! GameJoined(playerId, newState)
        waitingForPlayers(newState, autoFoldService, sessionRegistry, self)

    case StartGame(replyTo) =>
      if state.players.size < 2 then
        replyTo ! Error("Not enough players to start")
        Behaviors.same
      else
        val newState = PokerEngine.startNewHand(state)
        replyTo ! GameStarted(newState)
        notifyAutoFold(state.id, newState, autoFoldService, sessionRegistry, self)
        playing(newState, autoFoldService, sessionRegistry, self)

    case GetState(replyTo) =>
      replyTo ! state
      Behaviors.same

    case LeaveGame(playerId, replyTo) =>
      val newState = state.copy(players = state.players.filterNot(_.id == playerId))
      replyTo ! ActionSuccess(newState)
      waitingForPlayers(newState, autoFoldService, sessionRegistry, self)

    case UpdateSettings(newSettings, replyTo) =>
      val updatedState = state.copy(settings = newSettings)
      replyTo ! SettingsUpdated(updatedState)
      waitingForPlayers(updatedState, autoFoldService, sessionRegistry, self)

    case _ =>
      Behaviors.unhandled
  }

  // ── Phase: between hands ───────────────────────────────────────────────────

  private def betweenHands(
                            state: GameState,
                            autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                            sessionRegistry: Option[ActorRef[SessionRegistry.Command]],
                            self: ActorRef[Command]
                          ): Behavior[Command] = Behaviors.receiveMessage {

    case JoinGame(_, _, replyTo) =>
      replyTo ! Error("Cannot join after the game has started")
      Behaviors.same

    case StartGame(replyTo) =>
      if state.status == GameStatus.Finished then
        replyTo ! Error("Tournament is over")
        Behaviors.same
      else if state.players.count(_.chips > 0) < 2 then
        replyTo ! Error("Not enough players with chips to continue")
        Behaviors.same
      else
        val newState = PokerEngine.startNewHand(state)
        replyTo ! GameStarted(newState)
        notifyAutoFold(state.id, newState, autoFoldService, sessionRegistry, self)
        playing(newState, autoFoldService, sessionRegistry, self)

    case GetState(replyTo) =>
      replyTo ! state
      Behaviors.same

    case UpdateSettings(newSettings, replyTo) =>
      val updatedState = state.copy(settings = newSettings)
      replyTo ! SettingsUpdated(updatedState)
      betweenHands(updatedState, autoFoldService, sessionRegistry, self)

    case _ =>
      Behaviors.unhandled
  }

  // ── Phase: playing a hand ──────────────────────────────────────────────────

  private def playing(
                       state: GameState,
                       autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                       sessionRegistry: Option[ActorRef[SessionRegistry.Command]],
                       self: ActorRef[Command]
                     ): Behavior[Command] = Behaviors.receiveMessage {

    case GetState(replyTo) =>
      replyTo ! state
      Behaviors.same

    case fold: Fold =>
      handleAction(state, fold.playerId, fold.replyTo, autoFoldService, sessionRegistry, self) { (p, st) =>
        st.updatePlayer(p.copy(isActive = false, hasActed = true))
      }

    case check: Check =>
      handleAction(state, check.playerId, check.replyTo, autoFoldService, sessionRegistry, self) { (p, st) =>
        if p.currentBet < st.currentHighestBet then
          throw new IllegalArgumentException("Cannot check, must call or raise")
        st.updatePlayer(p.copy(hasActed = true))
      }

    case call: Call =>
      handleAction(state, call.playerId, call.replyTo, autoFoldService, sessionRegistry, self) { (p, st) =>
        val amountToCall = st.currentHighestBet - p.currentBet
        val actualCall = math.min(amountToCall, p.chips)
        st.updatePlayer(p.copy(
          chips = p.chips - actualCall,
          currentBet = p.currentBet + actualCall,
          hasActed = true
        ))
      }

    case raise: Raise =>
      handleAction(state, raise.playerId, raise.replyTo, autoFoldService, sessionRegistry, self) { (p, st) =>
        val amountToCall = st.currentHighestBet - p.currentBet
        val totalAmount = amountToCall + raise.amount
        if totalAmount > p.chips then
          throw new IllegalArgumentException("Not enough chips to raise")
        st.updatePlayer(p.copy(
          chips = p.chips - totalAmount,
          currentBet = p.currentBet + totalAmount,
          hasActed = true
        )).copy(currentHighestBet = st.currentHighestBet + raise.amount)
      }

    case StartGame(replyTo) =>
      replyTo ! Error("Game already in progress")
      Behaviors.same

    case LeaveGame(playerId, replyTo) =>
      replyTo ! Error("Cannot leave during a hand. Wait for the hand to end.")
      Behaviors.same

    case _ => Behaviors.unhandled
  }

  // ── Central action handler ─────────────────────────────────────────────────

  private def handleAction(
                            state: GameState,
                            playerId: String,
                            replyTo: ActorRef[Response],
                            autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                            sessionRegistry: Option[ActorRef[SessionRegistry.Command]],
                            self: ActorRef[Command]
                          )(action: (Player, GameState) => GameState): Behavior[Command] =
    state.currentPlayer.filter(_.id == playerId) match
      case None =>
        replyTo ! Error("Not your turn")
        Behaviors.same
      case Some(currentPlayer) =>
        try
          val stAfterAction = action(currentPlayer, state)
          val stWithNextTurn = stAfterAction.advanceTurn()

          val finalState =
            if stWithNextTurn.isBettingRoundOver then PokerEngine.advancePhase(stWithNextTurn)
            else stWithNextTurn

          if finalState.status == GameStatus.Finished then
            val winnerId = finalState.players.find(_.chips > 0).map(_.id).getOrElse("unknown")
            replyTo ! GameOver(winnerId, finalState)
            autoFoldService.foreach(_ ! AutoFoldService.CancelAutoFold(state.id))
            betweenHands(finalState, autoFoldService, sessionRegistry, self)
          else if finalState.status == GameStatus.WaitingForPlayers then
            replyTo ! ActionSuccess(finalState)
            autoFoldService.foreach(_ ! AutoFoldService.CancelAutoFold(state.id))
            betweenHands(finalState, autoFoldService, sessionRegistry, self)
          else
            replyTo ! ActionSuccess(finalState)
            notifyAutoFold(state.id, finalState, autoFoldService, sessionRegistry, self)
            playing(finalState, autoFoldService, sessionRegistry, self)
        catch
          case e: Exception =>
            replyTo ! Error(e.getMessage)
            Behaviors.same