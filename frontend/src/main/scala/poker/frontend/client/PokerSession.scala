package poker.frontend.client

import poker.domain.*
import poker.protocol.{ClientMessage, ServerMessage, PublicGameInfo}
import scalafx.application.Platform
import poker.frontend.ScenesNavigator

import java.util.concurrent.atomic.AtomicReference
import java.util.prefs.Preferences
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class PokerSessionService(
   client: PokerClientApi,
   runLater: (() => Unit) => Unit = action => Platform.runLater(action())
 ) {
  private given ExecutionContext = ExecutionContext.global

  private case class SessionState(
                                   playerName: String = "Player",
                                   playerId: Option[String] = None,
                                   identified: Boolean = false,
                                   connecting: Boolean = false,
                                   currentCode: Option[String] = None,
                                   currentGameState: Option[ClientGameState] = None,
                                   identityLocked: Boolean = false,
                                   pending: Vector[() => Unit] = Vector.empty
                                 )

  private val prefs = Preferences.userNodeForPackage(classOf[PokerSessionService])

  private val stateRef = AtomicReference(SessionState())

  private val stateHandlerRef =
    AtomicReference[(String, String, ClientGameState) => Unit]((_, _, _) => ())

  private val errorHandlerRef =
    AtomicReference[String => Unit](msg => println(s"Server error: $msg"))

  private val publicGamesHandlerRef =
    AtomicReference[List[PublicGameInfo] => Unit](_ => ())

  client.onMessage(handleMessage)

  def configure(name: String, stateHandler: (String, String, ClientGameState) => Unit,
                errorHandler: String => Unit = msg => println(s"Server error: $msg"))
  : Unit = {
    stateRef.updateAndGet {
      state =>
        val requestedName = name.trim

        val cleanedName = if state.identityLocked then state.playerName
                          else if requestedName.nonEmpty then requestedName
                          else state.playerName
        val samePlayer = cleanedName == state.playerName
        val storedPlayerId = Option(prefs.get(s"playerId.$cleanedName", null))
        val playerId = if samePlayer then state.playerId.orElse(storedPlayerId) else storedPlayerId

        state.copy(
          playerName = cleanedName,
          playerId = playerId,
          identified = samePlayer && state.identified,
          connecting = samePlayer && state.connecting,
          currentCode = if samePlayer then state.currentCode else None,
          currentGameState = if samePlayer then state.currentGameState else None,
          identityLocked = state.identityLocked || requestedName.nonEmpty,
          pending = if samePlayer then state.pending else Vector.empty
        )
    }

    stateHandlerRef.set(stateHandler)
    errorHandlerRef.set(errorHandler)
  }

  def currentPlayerName: String =
    stateRef.get().playerName

  def isPlayerNameLocked: Boolean =
    stateRef.get().identityLocked

  def currentView: Option[(String, String, ClientGameState)] = {
    val state = stateRef.get()
    for {
      code <- state.currentCode
      playerId <- state.playerId
      gameState <- state.currentGameState
    } yield (code, playerId, gameState)
  }

  def createGame(settings: GameSettings): Unit = {
    afterIdentify {
      client.send(ClientMessage.CreateGame(settings))
    }
  }

  def joinGame(code: String): Unit = {
    afterIdentify {
      client.send(ClientMessage.JoinGame(code.trim.toLowerCase))
    }
  }

  def listPublicGames(name: String, onLoaded: List[PublicGameInfo] => Unit, errorHandler: String => Unit): Unit = {
    configure(name, ScenesNavigator.showServerState, errorHandler)
    publicGamesHandlerRef.set(onLoaded)
    afterIdentify {
      client.send(ClientMessage.ListPublicGames())
    }
  }

  def startGame(): Unit = {
    sendWithCode(ClientMessage.StartGame.apply)
  }

  def fold(): Unit = {
    sendWithCode(ClientMessage.Fold.apply)
  }

  def check(): Unit = {
    sendWithCode(ClientMessage.Check.apply)
  }

  def call(): Unit = {
    sendWithCode(ClientMessage.Call.apply)
  }

  def raise(amount: Int): Unit =
    stateRef.get().currentCode.foreach { code =>
      client.send(ClientMessage.Raise(code, amount))
    }

  def leaveGame(): Unit = {
    val prevCode = stateRef.get().currentCode

    prevCode.foreach {
      code => client.send(ClientMessage.LeaveGame(code))
    }

    stateRef.updateAndGet(_.copy(
      currentCode = None,
      currentGameState = None
    ))
  }

  def updateSettings(settings: GameSettings): Unit = {
    sendWithCode(code => ClientMessage.UpdateSettings(code, settings))
  }

  private def sendWithCode(build: String => ClientMessage): Unit =
    stateRef.get().currentCode.foreach { code =>
      client.send(build(code))
    }

  private def afterIdentify(action: => Unit): Unit = {
    val runNow = stateRef.get().identified

    if runNow then action
    else {
      stateRef.updateAndGet {
        state => state.copy(pending = state.pending :+ (() => action))
      }
      connectIfNeeded()
    }
  }

  private def connectIfNeeded(): Unit = {
    val shouldConnect = {
      stateRef.updateAndGet {
        state => if state.connecting then state
        else state.copy(connecting = true)
      }
    }.connecting

    if shouldConnect then {
      client.connect().onComplete {
        case Success(_) => {
          val state = stateRef.get()
          client.send(ClientMessage.Identify(state.playerName, state.playerId))
        }
        case Failure(ex) => {
          stateRef.updateAndGet(_.copy(connecting = false))
          runLater { () =>
            errorHandlerRef.get()(s"Connection failed: ${ex.getMessage}")
          }
        }
      }
    }
  }

  private def handleMessage(msg: ServerMessage): Unit = {
    msg match {
      case ServerMessage.Identified(id, _) => {
        val prevState = stateRef.get()
        val stateAfterIdentify = stateRef.updateAndGet {
          state => {
            prefs.put(s"playerId.${state.playerName}", id)
            state.copy(
              playerId = Some(id),
              identified = true,
              connecting = false,
              pending = Vector.empty
            )
          }
        }

        prevState.pending.foreach(_())
      }

      case ServerMessage.GameCreated(code, state) => showState(code, state)
      case ServerMessage.GameJoined(code, state) => showState(code, state)
      case ServerMessage.GameStarted(code, state) => showState(code, state)
      case ServerMessage.StateUpdate(code, state) => showState(code, state)
      case ServerMessage.SettingsUpdated(code, state) => showState(code, state)

      case ServerMessage.GameOver(code, winnerId, winnerName, state) => {
        stateRef.updateAndGet(_.copy(currentCode = Some(code), currentGameState = Some(state)))
        runLater { () =>
          ScenesNavigator.showGameResult(code, winnerId, winnerName, state)
        }
      }
      case ServerMessage.PublicGameList(games) => runLater { () => publicGamesHandlerRef.get()(games) }
      case ServerMessage.Error(msg) => runLater { () => errorHandlerRef.get()(msg) }

      case ServerMessage.PlayerJoined(code, playerId, name) =>
        stateRef.get().currentGameState.foreach { state =>
          val newPlayer = ClientPlayer(playerId, name, 0, 0, true, false, false)
          showState(code, state.copy(players = state.players :+ newPlayer))
        }

      case ServerMessage.PlayerLeft(code, playerId, _) =>
        stateRef.get().currentGameState.foreach { state =>
          showState(code, state.copy(players = state.players.filterNot(_.id == playerId)))
        }

      case ServerMessage.PlayerDisconnected(code, playerId, _) =>
        stateRef.get().currentGameState.foreach { state =>
          if state.status == poker.domain.GameStatus.WaitingForPlayers then
            showState(code, state.copy(players = state.players.filterNot(_.id == playerId)))
          else
            showState(code, state.copy(players = state.players.map(p => if p.id == playerId then p.copy(isActive = false) else p)))
        }

      case ServerMessage.PlayerReconnected(code, playerId, name) =>
        stateRef.get().currentGameState.foreach { state =>
          val updatedPlayers = if state.players.exists(_.id == playerId) then
            state.players.map(p => if p.id == playerId then p.copy(isActive = true) else p)
          else
            state.players :+ ClientPlayer(playerId, name, 0, 0, true, false, false)
          showState(code, state.copy(players = updatedPlayers))
        }
      case _ => ()
    }
  }

  private def showState(code: String, gameState: ClientGameState): Unit = {
    val state = stateRef.updateAndGet(_.copy(currentCode = Some(code), currentGameState = Some(gameState)))

    val id = state.playerId.getOrElse("")

    runLater {() => stateHandlerRef.get()(code, id, gameState)}
  }
}

object PokerSession {
  private val serverUrl = sys.props.get("poker.serverUrl")
    .orElse(sys.env.get("POKER_WS_URL"))
    .getOrElse("ws://localhost:8080/ws")

  private val service = new PokerSessionService(new PokerClient(serverUrl))

  def configure(
                 name: String,
                 stateHandler: (String, String, ClientGameState) => Unit,
                 errorHandler: String => Unit = msg => println(s"Server error: $msg")
               ): Unit =
    service.configure(name, stateHandler, errorHandler)

  def currentPlayerName: String =
    service.currentPlayerName

  def isPlayerNameLocked: Boolean =
    service.isPlayerNameLocked

  def currentView: Option[(String, String, ClientGameState)] =
    service.currentView

  def createGame(settings: GameSettings): Unit =
    service.createGame(settings)

  def joinGame(code: String): Unit =
    service.joinGame(code)

  def startGame(): Unit =
    service.startGame()

  def fold(): Unit =
    service.fold()

  def check(): Unit =
    service.check()

  def call(): Unit =
    service.call()

  def raise(amount: Int): Unit =
    service.raise(amount)

  def leaveGame(): Unit =
    service.leaveGame()

  def listPublicGames(name: String, onLoaded: List[PublicGameInfo] => Unit, errorHandler: String => Unit): Unit =
    service.listPublicGames(name, onLoaded, errorHandler)

  def updateSettings(settings: GameSettings): Unit =
    service.updateSettings(settings)
}
