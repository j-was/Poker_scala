package poker.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ClientViewSpec extends AnyFlatSpec with Matchers {

  private val settings = GameSettings(smallBlind = 10, bigBlind = 20, initialChips = 1000)

  private def makeState(p1Cards: HoleCards, p2Cards: HoleCards, board: Board = Board.PreFlop): GameState =
    GameState(
      id = "test",
      settings = settings,
      board = board,
      players = List(
        Player("p1", "Alice", 980, Some(p1Cards)),
        Player("p2", "Bob", 980, Some(p2Cards))
      )
    )

  "toClientView" should "hide other players hole cards" in {
    val state = makeState(
      HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Spades)),
      HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds))
    )
    val view = state.toClientView("p1")

    view.players.find(_.id == "p1").get.hasCards shouldBe true
    view.players.find(_.id == "p2").get.hasCards shouldBe true
    view.myHoleCards.isDefined shouldBe true
    view.myHoleCards.get.c1 shouldBe Card(Rank.Ace, Suit.Spades)
  }

  it should "not expose myHoleCards for a spectator (unknown playerId)" in {
    val state = makeState(
      HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Spades)),
      HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds))
    )
    val view = state.toClientView("spectator-99")

    view.myHoleCards shouldBe None
    view.myHandCategory shouldBe None
    view.myBestCards shouldBe None
  }

  it should "include hand category when hole cards and board are available" in {
    val state = makeState(
      HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)),
      HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds)),
      board = Board.River(
        Card(Rank.Ace, Suit.Clubs), Card(Rank.Ace, Suit.Diamonds), Card(Rank.King, Suit.Spades),
        Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Spades)
      )
    )
    val view = state.toClientView("p1")

    view.myHandCategory shouldBe Some(HandCategory.FourOfAKind)
    view.myBestCards.isDefined shouldBe true
    view.myBestCards.get.size shouldBe 5
  }

  it should "include hand category even on PreFlop (partial evaluation)" in {
    val state = makeState(
      HoleCards(Card(Rank.King, Suit.Spades), Card(Rank.King, Suit.Hearts)),
      HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds))
    )
    val view = state.toClientView("p1")

    view.myHandCategory shouldBe Some(HandCategory.OnePair)
    view.myBestCards.get shouldBe List(Card(Rank.King, Suit.Spades), Card(Rank.King, Suit.Hearts))
  }

  it should "not include hand category when player has no cards" in {
    val state = GameState(
      id = "test",
      settings = settings,
      players = List(Player("p1", "Alice", 1000, holeCards = None))
    )
    val view = state.toClientView("p1")

    view.myHoleCards shouldBe None
    view.myHandCategory shouldBe None
    view.myBestCards shouldBe None
  }
}
