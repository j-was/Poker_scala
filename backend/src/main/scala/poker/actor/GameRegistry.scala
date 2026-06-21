package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.GameSettings
import poker.protocol.PublicGameInfo

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

  case class ListPublicGames(replyTo: ActorRef[PublicGameListResult]) extends Command

  case class GameNameExists(name: String, replyTo: ActorRef[Boolean]) extends Command

  case class PublicGameListResult(games: List[PublicGameInfo])


  sealed trait CreateResult

  case class GameCreated(code: String, ref: ActorRef[GameInstance.Command]) extends CreateResult

  sealed trait LookupResult

  case class Found(code: String, ref: ActorRef[GameInstance.Command]) extends LookupResult

  case object NotFound extends LookupResult

  def apply(
             autoFoldService: Option[ActorRef[AutoFoldService.Command]] = None,
             sessionRegistry: Option[ActorRef[SessionRegistry.Command]] = None
           ): Behavior[Command] = registry(Map.empty, autoFoldService, sessionRegistry)

  case class GameMeta(
                       ref: ActorRef[GameInstance.Command],
                       settings: GameSettings,
                       playerCount: Int
                     )

  private def registry(
                        games: Map[String, GameMeta],
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
        registry(
          games + (code -> GameMeta(ref, settings, 1)),
          autoFoldService,
          sessionRegistry
        )

      case ListPublicGames(replyTo) =>
        val publicGames = games.collect {
          case (code, GameMeta(_, settings, count)) if settings.isPublic =>
            PublicGameInfo(
              code = code,
              name = settings.name,
              playerCount = count,
              maxPlayers = settings.maxPlayers,
              smallBlind = settings.smallBlind,
              bigBlind = settings.bigBlind,
              isPublic = true
            )
        }.toList
        replyTo ! PublicGameListResult(publicGames)
        Behaviors.same

      case GameNameExists(name, replyTo) =>
        replyTo ! games.values.exists(_.settings.name == name)
        Behaviors.same

      // ... rest of existing cases, update RemoveGame:
      case RemoveGame(code) =>
        registry(games - code, autoFoldService, sessionRegistry)
    }
  }
}