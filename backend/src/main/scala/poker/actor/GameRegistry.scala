package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.GameSettings
import poker.protocol.PublicGameInfo

import scala.util.Random

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
  case class PlayerHasGame(playerId: String, replyTo: ActorRef[Boolean]) extends Command  // NEW
  case class PublicGameListResult(games: List[PublicGameInfo])


  sealed trait CreateResult
  case class GameCreated(code: String, ref: ActorRef[GameInstance.Command]) extends CreateResult
  case class AlreadyHosting(code: String) extends CreateResult  // NEW - clear error message
  sealed trait LookupResult
  case class Found(code: String, ref: ActorRef[GameInstance.Command]) extends LookupResult
  case object NotFound extends LookupResult

  def apply(
             autoFoldService: Option[ActorRef[AutoFoldService.Command]] = None,
             sessionRegistry: Option[ActorRef[SessionRegistry.Command]] = None
           ): Behavior[Command] = registry(Map.empty, Map.empty, autoFoldService, sessionRegistry)

  case class GameMeta(
                       ref: ActorRef[GameInstance.Command],
                       settings: GameSettings,
                       hostId: String,
                       playerCount: Int
                     )

  private def registry(
                        games: Map[String, GameMeta],
                        hostGames: Map[String, String],  // NEW - track which game each host owns
                        autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                        sessionRegistry: Option[ActorRef[SessionRegistry.Command]]
                      ): Behavior[Command] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case CreateGame(requesterId, settings, replyTo) =>
        hostGames.get(requesterId) match
          case Some(existingCode) =>
            games.get(existingCode) match
              case Some(meta) if meta.playerCount <= 1 =>
                ctx.stop(meta.ref)
                sessionRegistry.foreach { registry =>
                  registry ! SessionRegistry.GameRemoved(existingCode)
                }
                ctx.log.info(s"Dissolved game '$existingCode' (host creating new game)")
                createNewGame(requesterId, settings, replyTo, games - existingCode, hostGames - requesterId, autoFoldService, sessionRegistry, ctx)
              case Some(_) =>
                replyTo ! AlreadyHosting(existingCode)
                Behaviors.same
              case None =>
                createNewGame(requesterId, settings, replyTo, games, hostGames - requesterId, autoFoldService, sessionRegistry, ctx)
          case None =>
            createNewGame(requesterId, settings, replyTo, games, hostGames, autoFoldService, sessionRegistry, ctx)

      case LookupGame(code, replyTo) =>
        games.get(code) match
          case Some(meta) => replyTo ! Found(code, meta.ref)
          case None => replyTo ! NotFound
        Behaviors.same

      case ListPublicGames(replyTo) =>
        val publicGames = games.collect {
          case (code, GameMeta(_, settings, _, count)) if settings.isPublic =>
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

      case PlayerHasGame(playerId, replyTo) =>
        replyTo ! hostGames.contains(playerId)
        Behaviors.same

      case RemoveGame(code) =>
        val updatedHostGames = games.get(code) match
          case Some(meta) => hostGames - meta.hostId
          case None => hostGames

        games.get(code).foreach { meta =>
          ctx.stop(meta.ref)
        }

        sessionRegistry.foreach { registry =>
          registry ! SessionRegistry.GameRemoved(code)
        }

        ctx.log.info(s"Game '$code' fully removed and cleaned up")
        registry(games - code, updatedHostGames, autoFoldService, sessionRegistry)
    }
  }

  private def createNewGame(
                             requesterId: String,
                             settings: GameSettings,
                             replyTo: ActorRef[CreateResult],
                             games: Map[String, GameMeta],
                             hostGames: Map[String, String],
                             autoFoldService: Option[ActorRef[AutoFoldService.Command]],
                             sessionRegistry: Option[ActorRef[SessionRegistry.Command]],
                             ctx: org.apache.pekko.actor.typed.scaladsl.ActorContext[Command]
                           ): Behavior[Command] = {
    val code = generateCode(games.keySet)
    val ref = ctx.spawn(
      GameInstance(code, settings, autoFoldService, sessionRegistry),
      s"game-$code"
    )
    ctx.watchWith(ref, RemoveGame(code))
    replyTo ! GameCreated(code, ref)
    registry(
      games + (code -> GameMeta(ref, settings, requesterId, 1)),
      hostGames + (requesterId -> code),
      autoFoldService,
      sessionRegistry
    )
  }
}