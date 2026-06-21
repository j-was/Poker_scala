package poker.protocol

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe.*
import io.circe.syntax.*
import io.circe.parser.*
import poker.domain.*
import poker.protocol.JsonCodecs.*
import poker.protocol.JsonCodecs.given


class ProtocolSpec extends AnyFlatSpec with Matchers {

  private def roundtrip[A](value: A)(using Encoder[A], Decoder[A]): Unit = {
    val json = value.asJson
    val decoded = json.as[A]
    decoded shouldBe Right(value)
  }

  "JsonCodecs" should "serialize and deserialize Suit" in {
    Suit.values.foreach { suit =>
      roundtrip(suit)
    }
    // Test invalid suit decoding
    Json.fromString("X").as[Suit].isLeft shouldBe true
  }

  it should "serialize and deserialize Rank" in {
    Rank.values.foreach { rank =>
      roundtrip(rank)
    }
    // Test invalid rank decoding
    Json.fromString("X").as[Rank].isLeft shouldBe true
  }

  it should "serialize and deserialize Card" in {
    roundtrip(Card(Rank.Ace, Suit.Spades))
    roundtrip(Card(Rank.Ten, Suit.Hearts))
  }

  it should "serialize and deserialize HoleCards" in {
    roundtrip(HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts)))
  }

  it should "serialize and deserialize HandCategory" in {
    HandCategory.values.foreach { cat =>
      roundtrip(cat)
    }
    Json.fromString("InvalidCategory").as[HandCategory].isLeft shouldBe true
  }

  it should "serialize and deserialize GameStatus" in {
    GameStatus.values.foreach { status =>
      roundtrip(status)
    }
    Json.fromString("InvalidStatus").as[GameStatus].isLeft shouldBe true
  }

  it should "serialize and deserialize GameSettings" in {
    roundtrip(GameSettings(name = "Test", smallBlind = 5, bigBlind = 10, initialChips = 500, isPublic = true, maxPlayers = 5))
  }

  it should "serialize and deserialize PublicGameInfo" in {
    roundtrip(PublicGameInfo(code = "abc", name = "Test", playerCount = 3, maxPlayers = 6, smallBlind = 10, bigBlind = 20, isPublic = true))
  }

  it should "serialize and deserialize Board" in {
    roundtrip[Board](Board.PreFlop)

    val flop = Board.Flop(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts), Card(Rank.Queen, Suit.Diamonds))
    roundtrip[Board](flop)

    val turn = Board.Turn(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts), Card(Rank.Queen, Suit.Diamonds), Card(Rank.Jack, Suit.Clubs))
    roundtrip[Board](turn)

    val river = Board.River(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts), Card(Rank.Queen, Suit.Diamonds), Card(Rank.Jack, Suit.Clubs), Card(Rank.Ten, Suit.Spades))
    roundtrip[Board](river)

    // Test invalid board decoding
    Json.obj("street" -> "InvalidStreet".asJson).as[Board].isLeft shouldBe true
  }

  it should "serialize and deserialize ClientPlayer" in {
    roundtrip(ClientPlayer(id = "p1", name = "Alice", chips = 100, currentBet = 20, isActive = true, hasActed = true, hasCards = true))
  }

  it should "serialize and deserialize ClientGameState" in {
    val state = ClientGameState(
      id = "game-1",
      status = GameStatus.Playing,
      settings = GameSettings(),
      board = Board.PreFlop,
      players = List(ClientPlayer("p1", "Alice", 1000, 0, true, false, true)),
      pot = 30,
      dealerIndex = 0,
      currentPlayerIndex = 0,
      currentHighestBet = 20,
      myHoleCards = Some(HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts))),
      myHandCategory = Some(HandCategory.OnePair),
      myBestCards = Some(List(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts)))
    )
    roundtrip(state)
  }

  it should "serialize and deserialize ClientMessage" in {
    roundtrip[ClientMessage](ClientMessage.Identify("Alice", Some("p1")))
    roundtrip[ClientMessage](ClientMessage.Identify("Alice", None))
    roundtrip[ClientMessage](ClientMessage.CreateGame(GameSettings()))
    roundtrip[ClientMessage](ClientMessage.JoinGame("abc"))
    roundtrip[ClientMessage](ClientMessage.StartGame("abc"))
    roundtrip[ClientMessage](ClientMessage.Fold("abc"))
    roundtrip[ClientMessage](ClientMessage.Call("abc"))
    roundtrip[ClientMessage](ClientMessage.Check("abc"))
    roundtrip[ClientMessage](ClientMessage.Raise("abc", 50))
    roundtrip[ClientMessage](ClientMessage.LeaveGame("abc"))
    roundtrip[ClientMessage](ClientMessage.ListPublicGames())
    roundtrip[ClientMessage](ClientMessage.UpdateSettings("abc", GameSettings()))
    roundtrip[ClientMessage](ClientMessage.Ping)

    // Invalid client message type decoding
    Json.obj("type" -> "InvalidMsg".asJson).as[ClientMessage].isLeft shouldBe true
  }

  it should "serialize and deserialize ServerMessage" in {
    val dummyState = ClientGameState(
      id = "game-1", status = GameStatus.WaitingForPlayers, settings = GameSettings(), board = Board.PreFlop,
      players = Nil, pot = 0, dealerIndex = 0, currentPlayerIndex = 0, currentHighestBet = 0,
      myHoleCards = None, myHandCategory = None, myBestCards = None
    )

    roundtrip[ServerMessage](ServerMessage.Identified("p1", "Alice"))
    roundtrip[ServerMessage](ServerMessage.GameCreated("abc", dummyState))
    roundtrip[ServerMessage](ServerMessage.GameJoined("abc", dummyState))
    roundtrip[ServerMessage](ServerMessage.GameStarted("abc", dummyState))
    roundtrip[ServerMessage](ServerMessage.StateUpdate("abc", dummyState))
    roundtrip[ServerMessage](ServerMessage.GameOver("abc", "p1", "Alice", dummyState))
    roundtrip[ServerMessage](ServerMessage.PlayerJoined("abc", "p1", "Alice"))
    roundtrip[ServerMessage](ServerMessage.PlayerLeft("abc", "p1", "Alice"))
    roundtrip[ServerMessage](ServerMessage.PlayerDisconnected("abc", "p1", "Alice"))
    roundtrip[ServerMessage](ServerMessage.PlayerReconnected("abc", "p1", "Alice"))
    roundtrip[ServerMessage](ServerMessage.Error("some error"))
    roundtrip[ServerMessage](ServerMessage.Pong)
    roundtrip[ServerMessage](ServerMessage.PublicGameList(List(PublicGameInfo("abc", "Name", 1, 6, 10, 20, true))))
    roundtrip[ServerMessage](ServerMessage.SettingsUpdated("abc", dummyState))

    // Invalid server message type decoding
    Json.obj("type" -> "InvalidMsg".asJson).as[ServerMessage].isLeft shouldBe true
  }
}
