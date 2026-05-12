package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.GameSettings
import scala.util.Random

/**
 * Top-level registry that owns all active game instances.
 */
object GameRegistry {

  private val adjectives = Vector(
    "swift", "brave", "calm", "dark", "epic", "wild", "bold", "keen",
    "vast", "warm", "cool", "sharp", "lucky", "quick", "silver", "golden",
    "silent", "bright", "fierce", "noble"
  )
  private val nouns = Vector(
    "fox", "wolf", "hawk", "bear", "lion", "tiger", "eagle", "shark",
    "raven", "cobra", "panda", "moose", "bison", "lynx", "falcon", "otter",
    "viper", "crane", "finch", "hound"
  )

  private def generateCode(existing: Set[String]): String = {
    var code = ""
    while code.isEmpty || existing.contains(code) do
      val adj = adjectives(Random.nextInt(adjectives.size))
      val noun = nouns(Random.nextInt(nouns.size))
      val digits = 100 + Random.nextInt(900)
      code = s"$adj-$noun-$digits"
    code
  }
  
  sealed trait Command
  case class CreateGame(
                         requesterId: String,
                         settings: GameSettings,
                         replyTo: ActorRef[CreateResult]
                       ) extends Command
  case class LookupGame(
                         code: String,
                         replyTo: ActorRef[LookupResult]
                       ) extends Command
  case class RemoveGame(code: String) extends Command
  

  sealed trait CreateResult
  case class GameCreated(code: String, ref: ActorRef[GameInstance.Command]) extends CreateResult
  sealed trait LookupResult
  case class Found(code: String, ref: ActorRef[GameInstance.Command]) extends LookupResult
  case object NotFound extends LookupResult
  
  def apply(
             autoFoldService: Option[ActorRef[AutoFoldService.Command]] = None,
             sessionRegistry: Option[ActorRef[SessionRegistry.Command]] = None
           ): Behavior[Command] = registry(Map.empty, autoFoldService, sessionRegistry)

  private def registry(
                        games: Map[String, ActorRef[GameInstance.Command]],
                        autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                        sessionRegistry: Option[ActorRef[SessionRegistry.Command]]
                      ): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case CreateGame(_, settings, replyTo) =>
        val code = generateCode(games.keySet)
        val ref = ctx.spawn(
          GameInstance(code, settings, autoFoldService, sessionRegistry),
          s"game-$code"
        )
        ctx.watchWith(ref, RemoveGame(code))
        replyTo ! GameCreated(code, ref)
        registry(games + (code -> ref), autoFoldService, sessionRegistry)

      case LookupGame(code, replyTo) =>
        games.get(code) match
          case Some(ref) => replyTo ! Found(code, ref)
          case None => replyTo ! NotFound
        Behaviors.same

      case RemoveGame(code) =>
        ctx.log.info(s"Game '$code' actor terminated; removing from registry")
        registry(games - code, autoFoldService, sessionRegistry)
    }
  }
}