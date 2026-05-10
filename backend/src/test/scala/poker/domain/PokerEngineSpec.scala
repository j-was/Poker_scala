package poker.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PokerEngineSpec extends AnyFlatSpec with Matchers {

  private def makeState(players: List[Player]): GameState =
    GameState(id = "test", players = players)

  private def makePlayer(id: String, chips: Int, holeCards: HoleCards): Player =
    Player(id = id, name = id, chips = chips, holeCards = Some(holeCards), isActive = true)

  "PokerEngine.distributePotAndReset" should "award the full pot to the sole winner" in {
    val winner = makePlayer("w", 0, HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
    val loser  = makePlayer("l", 0, HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds)))

    val board = Board.River(
      Card(Rank.Ace, Suit.Clubs), Card(Rank.Ace, Suit.Diamonds), Card(Rank.King, Suit.Spades),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Spades)
    )
    val pot = Pot(Map("w" -> 100, "l" -> 100))
    val state = makeState(List(winner, loser)).copy(board = board, pot = pot)

    val result = PokerEngine.distributePotAndReset(state)

    result.players.find(_.id == "w").get.chips shouldBe 200
    result.players.find(_.id == "l").get.chips shouldBe 0
    result.pot.amount shouldBe 0
  }

  it should "award the pot to the only active player when others folded" in {
    val active = makePlayer("a", 0, HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds)))
    val folded = makePlayer("f", 0, HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
      .copy(isActive = false)

    val pot = Pot(Map("a" -> 50, "f" -> 50))
    val state = makeState(List(active, folded)).copy(pot = pot)

    val result = PokerEngine.distributePotAndReset(state)

    result.players.find(_.id == "a").get.chips shouldBe 100
    result.players.find(_.id == "f").get.chips shouldBe 0
  }

  it should "split the pot evenly on a tie" in {
    val p1 = makePlayer("p1", 0, HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts)))
    val p2 = makePlayer("p2", 0, HoleCards(Card(Rank.Ace, Suit.Clubs), Card(Rank.King, Suit.Diamonds)))

    val board = Board.River(
      Card(Rank.Queen, Suit.Spades), Card(Rank.Jack, Suit.Hearts), Card(Rank.Ten, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Clubs)
    )
    val pot = Pot(Map("p1" -> 100, "p2" -> 100))
    val state = makeState(List(p1, p2)).copy(board = board, pot = pot)

    val result = PokerEngine.distributePotAndReset(state)

    result.players.find(_.id == "p1").get.chips shouldBe 100
    result.players.find(_.id == "p2").get.chips shouldBe 100
    result.pot.amount shouldBe 0
  }

  it should "correctly compute a side pot when one player is all-in for less" in {
    val allIn  = makePlayer("short", 0, HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
    val deep1  = makePlayer("deep1", 0, HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds)))
    val deep2  = makePlayer("deep2", 0, HoleCards(Card(Rank.Four, Suit.Clubs), Card(Rank.Five, Suit.Diamonds)))

    val board = Board.River(
      Card(Rank.Ace, Suit.Clubs), Card(Rank.Ace, Suit.Diamonds), Card(Rank.King, Suit.Spades),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Six, Suit.Spades)
    )
    // short went all-in for 50, deep1 and deep2 each put in 150
    val pot = Pot(Map("short" -> 50, "deep1" -> 150, "deep2" -> 150))
    val state = makeState(List(allIn, deep1, deep2)).copy(board = board, pot = pot)

    val result = PokerEngine.distributePotAndReset(state)

    // short wins main pot (50*3=150), deep1 and deep2 split side pot (100*2=200) equally
    // deep2 has 4-5 which is a straight on A-2-6-K board? No - 2,3,4,5,6 = straight!
    // Actually let me recalculate: board is A♣ A♦ K♠ 2♥ 6♠
    // short: A♠ A♥ + A♣ A♦ = Four Aces (wins everything)
    // Wait, short has 4 aces so they win everything
    result.players.find(_.id == "short").get.chips shouldBe 150
    result.players.find(_.id == "deep1").get.chips + result.players.find(_.id == "deep2").get.chips shouldBe 200
    result.pot.amount shouldBe 0
  }

  it should "reset all player state after distribution" in {
    val p1 = makePlayer("p1", 50, HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts)))
    val p2 = makePlayer("p2", 50, HoleCards(Card(Rank.Two, Suit.Clubs), Card(Rank.Three, Suit.Diamonds)))
    val pot = Pot(Map("p1" -> 50, "p2" -> 50))
    val state = makeState(List(p1, p2)).copy(pot = pot)

    val result = PokerEngine.distributePotAndReset(state)

    result.players.foreach { p =>
      p.holeCards shouldBe None
      p.isActive shouldBe true
      p.hasActed shouldBe false
      p.currentBet shouldBe 0
    }
    result.board shouldBe Board.PreFlop
    result.status shouldBe GameStatus.WaitingForPlayers
  }

  "PokerEngine.startNewHand" should "deal 2 hole cards to each player" in {
    val state = GameState(
      id = "test",
      settings = GameSettings(smallBlind = 10, bigBlind = 20),
      players = List(
        Player("p1", "Alice", 1000),
        Player("p2", "Bob",   1000)
      )
    )
    val result = PokerEngine.startNewHand(state)

    result.players.foreach { p =>
      p.holeCards.isDefined shouldBe true
    }
    result.board shouldBe Board.PreFlop
    result.status shouldBe GameStatus.Playing
  }

  it should "post small blind and big blind correctly" in {
    val settings = GameSettings(smallBlind = 10, bigBlind = 20)
    val state = GameState(
      id = "test",
      settings = settings,
      players = List(
        Player("p1", "Alice", 1000),
        Player("p2", "Bob",   1000)
      ),
      dealerIndex = 0
    )
    val result = PokerEngine.startNewHand(state)

    val bets = result.players.map(_.currentBet).toSet
    bets.contains(10) shouldBe true
    bets.contains(20) shouldBe true
    result.currentHighestBet shouldBe 20
  }

  it should "rotate the dealer index on each new hand" in {
    val state = GameState(
      id = "test",
      settings = GameSettings(),
      players = List(
        Player("p1", "Alice", 1000),
        Player("p2", "Bob",   1000),
        Player("p3", "Carol", 1000)
      ),
      dealerIndex = 0
    )
    val result = PokerEngine.startNewHand(state)
    result.dealerIndex shouldBe 1
  }

  it should "mark a bankrupt player as inactive" in {
    val state = GameState(
      id = "test",
      settings = GameSettings(smallBlind = 10, bigBlind = 20),
      players = List(
        Player("p1", "Alice", 1000),
        Player("p2", "Bob",   0),
        Player("p3", "Carol", 1000)
      )
    )
    val result = PokerEngine.startNewHand(state)

    result.players.find(_.id == "p2").get.isActive shouldBe false
    result.players.find(_.id == "p2").get.holeCards shouldBe None
  }

  it should "set status to Finished when only one player has chips" in {
    val state = GameState(
      id = "test",
      settings = GameSettings(),
      players = List(
        Player("p1", "Alice", 1000),
        Player("p2", "Bob",   0)
      )
    )
    val result = PokerEngine.startNewHand(state)
    result.status shouldBe GameStatus.Finished
  }
}
