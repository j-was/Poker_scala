package poker.frontend.client

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import poker.domain.*
import poker.domain.GameStatus.WaitingForPlayers
import poker.protocol.{ClientMessage, ServerMessage}

import scala.concurrent.Future

class PokerSessionSpec extends AnyFlatSpec with Matchers  {
  private final class FakePokerClient extends PokerClientApi {
    var sent: Vector[ClientMessage] = Vector.empty
    var connected: Boolean = false
    private var handler: ServerMessage => Unit = _ => ()

    override def send(msg: ClientMessage): Boolean = {
      sent = sent :+ msg
      true
    }

    override def connect(): Future[Unit] = {
      connected = true
      Future.successful(())
    }

    override def onMessage(handler: ServerMessage => Unit): Unit = {
      this.handler = handler
    }

    def receive(msg: ServerMessage): Unit = {
      handler(msg)
    }
  }
  
  private def dummyState: ClientGameState =
    ClientGameState(
      id = "game-1",
      status = WaitingForPlayers,
      settings = GameSettings(),
      board = Board.PreFlop,
      players = List(
        ClientPlayer(
          id = "p1",
          name = "John",
          chips = 1000,
          currentBet = 0,
          isActive = true,
          hasActed = false,
          hasCards = true
        )
      ),
      pot = 0,
      dealerIndex = 0,
      currentPlayerIndex = 0,
      currentHighestBet = 0,
      myHoleCards = None,
      myHandCategory = None,
      myBestCards = None
    )
    
  "PokerSessionService" should "send Identify before JoinGame and then send normalized JoinGame" in {
    val client = new FakePokerClient
    val session = new PokerSessionService(client, runLater = action => action())
    
    session.configure(name = "John", stateHandler = (_, _, _) => ())
    session.joinGame("ABC")

    client.connected shouldBe true

    client.sent should not contain ClientMessage.JoinGame("abc")

    client.receive(ServerMessage.Identified("p1", "John"))

    client.sent should contain(ClientMessage.JoinGame("abc"))
  }
  
  it should "send CreateGame with same settings and identification" in {
    val client = new FakePokerClient
    val session = new PokerSessionService(client, runLater = action => action())
    
    val settings = GameSettings(smallBlind = 25, bigBlind = 50, initialChips = 2000)

    session.configure(name = "John", stateHandler = (_, _, _) => ())
    session.createGame(settings)

    client.receive(ServerMessage.Identified("p1", "John"))

    client.sent should contain(ClientMessage.CreateGame(settings))
  }

  it should "use current game code for gameplay actions" in {
    val client = new FakePokerClient
    val session = new PokerSessionService(client, runLater = action => action())

    session.configure(
      name = "John",
      stateHandler = (_, _, _) => ()
    )

    session.joinGame("abc")
    client.receive(ServerMessage.Identified("p1", "John"))
    client.receive(ServerMessage.GameJoined("abc", dummyState))

    session.startGame()
    session.fold()
    session.check()
    session.call()
    session.raise(50)
    session.leaveGame()

    client.sent should contain(ClientMessage.StartGame("abc"))
    client.sent should contain(ClientMessage.Fold("abc"))
    client.sent should contain(ClientMessage.Check("abc"))
    client.sent should contain(ClientMessage.Call("abc"))
    client.sent should contain(ClientMessage.Raise("abc", 50))
    client.sent should contain(ClientMessage.LeaveGame("abc"))
  }

  it should "not send gameplay actions when there is no current game code" in {
    val client = new FakePokerClient
    val session = new PokerSessionService(client, runLater = action => action())

    session.configure(
      name = "John",
      stateHandler = (_, _, _) => ()
    )

    session.startGame()
    session.fold()
    session.check()
    session.call()
    session.raise(50)
    session.leaveGame()

    client.sent shouldBe empty
  }

  it should "call stateHandler after receiving game state from server" in {
    val client = new FakePokerClient
    var received: Option[(String, String, ClientGameState)] = None

    val session = new PokerSessionService(client, runLater = action => action())

    session.configure(
      name = "John",
      stateHandler = (code, playerId, state) => {
        received = Some((code, playerId, state))
      }
    )

    session.joinGame("abc")
    client.receive(ServerMessage.Identified("p1", "John"))
    client.receive(ServerMessage.GameJoined("abc", dummyState))

    received shouldBe Some(("abc", "p1", dummyState))
  }

  it should "call errorHandler after receiving Error from server" in {
    val client = new FakePokerClient
    var error: Option[String] = None

    val session = new PokerSessionService(client, runLater = action => action())

    session.configure(
      name = "John",
      stateHandler = (_, _, _) => (),
      errorHandler = msg => error = Some(msg)
    )

    client.receive(ServerMessage.Error("Game not found"))

    error shouldBe Some("Game not found")
  }
}
