package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.GameState
import poker.protocol.ServerMessage
import poker.domain.toClientView

/**
 * Tracks the live WebSocket send-channel for each connected player and
 * which game each player is in.
 */
object SessionRegistry {
  
  sealed trait Command

  case class Register(playerId: String, sender: ActorRef[ServerMessage]) extends Command

  case class Unregister(playerId: String) extends Command

  case class JoinedGame(playerId: String, gameCode: String) extends Command

  case class LeftGame(playerId: String, gameCode: String) extends Command

  case class SendTo(playerId: String, msg: ServerMessage) extends Command

  case class BroadcastToGame(
                              gameCode: String,
                              gameState: GameState,
                              buildMsg: (String, poker.domain.ClientGameState) => ServerMessage
                            ) extends Command

  case class BroadcastRaw(gameCode: String, msg: ServerMessage) extends Command

  case class IsConnectedResult(connected: Boolean)

  case class IsConnected(playerId: String, replyTo: ActorRef[IsConnectedResult]) extends Command


  def apply(): Behavior[Command] = registry(Map.empty, Map.empty)

  private def registry(
                        sessions: Map[String, ActorRef[ServerMessage]],
                        playerGame: Map[String, String]
                      ): Behavior[Command] = Behaviors.receiveMessage {

    case Register(playerId, sender) =>
      registry(sessions + (playerId -> sender), playerGame)
    case Unregister(playerId) =>
      registry(sessions - playerId, playerGame - playerId)
    case JoinedGame(playerId, gameCode) =>
      registry(sessions, playerGame + (playerId -> gameCode))
    case LeftGame(playerId, _) =>
      registry(sessions, playerGame - playerId)
    case SendTo(playerId, msg) =>
      sessions.get(playerId).foreach(_ ! msg)
      Behaviors.same
    case BroadcastToGame(gameCode, gameState, buildMsg) =>
      val playersInGame = playerGame.collect { case (pid, code) if code == gameCode => pid }
      playersInGame.foreach { pid =>
        sessions.get(pid).foreach { sender =>
          sender ! buildMsg(pid, gameState.toClientView(pid))
        }
      }
      Behaviors.same
    case BroadcastRaw(gameCode, msg) =>
      val playersInGame = playerGame.collect { case (pid, code) if code == gameCode => pid }
      playersInGame.foreach { pid => sessions.get(pid).foreach(_ ! msg) }
      Behaviors.same
    case IsConnected(playerId, replyTo) =>
      replyTo ! IsConnectedResult(sessions.contains(playerId))
      Behaviors.same
  }
}