package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.*

/**
 * Auto-folds a player who is disconnected when it's their turn.
 */

object AutoFoldService {

  val AutoFoldDelay: FiniteDuration = 30.seconds

  sealed trait Command

  case class TurnAdvanced(
                           gameCode: String,
                           playerId: String,
                           gameRef: ActorRef[GameInstance.Command],
                           sessions: ActorRef[SessionRegistry.Command]
                         ) extends Command

  case class CancelAutoFold(gameCode: String) extends Command

  private case class DoCheck(
                              gameCode: String,
                              playerId: String,
                              gameRef: ActorRef[GameInstance.Command],
                              sessions: ActorRef[SessionRegistry.Command]
                            ) extends Command

  private case class ConnectedResult(
                                      gameCode: String,
                                      playerId: String,
                                      gameRef: ActorRef[GameInstance.Command],
                                      isConnected: Boolean
                                    ) extends Command

  private case class DiscardResponse(r: GameInstance.Response) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.withTimers { timers =>
      Behaviors.setup { ctx =>

        val discardAdapter: ActorRef[GameInstance.Response] =
          ctx.messageAdapter[GameInstance.Response](DiscardResponse.apply)

        Behaviors.receiveMessage {

          case TurnAdvanced(gameCode, playerId, gameRef, sessions) => {
            timers.startSingleTimer(
              gameCode,
              DoCheck(gameCode, playerId, gameRef, sessions),
              AutoFoldDelay
            )
            Behaviors.same
          }

          case CancelAutoFold(gameCode) => {
            timers.cancel(gameCode)
            Behaviors.same
          }

          case DoCheck(gameCode, playerId, gameRef, sessions) => {
            val connAdapter = ctx.messageAdapter[SessionRegistry.IsConnectedResult] { r =>
              ConnectedResult(gameCode, playerId, gameRef, r.connected)
            }
            sessions ! SessionRegistry.IsConnected(playerId, connAdapter)
            Behaviors.same
          }

          case ConnectedResult(gameCode, playerId, gameRef, false) => {
            ctx.log.info(s"Auto-folding disconnected player '$playerId' in game '$gameCode'")
            gameRef ! GameInstance.Fold(playerId, discardAdapter)
            Behaviors.same
          }

          case ConnectedResult(_, _, _, true) =>
            Behaviors.same

          case DiscardResponse(_) =>
            Behaviors.same
        }
      }
    }
  }
}