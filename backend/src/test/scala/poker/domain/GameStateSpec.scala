package poker.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GameStateSpec extends AnyFlatSpec with Matchers {

  "GameState" should "correctly determine next player index" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000, isActive = true),
        Player("p2", "Bob", 1000, isActive = true),
        Player("p3", "Carol", 1000, isActive = false)
      )
    )

    state.nextPlayerIndex(0) shouldBe 1
    state.nextPlayerIndex(1) shouldBe 0
  }

  it should "return 0 if all players are inactive" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000, isActive = false),
        Player("p2", "Bob", 1000, isActive = false)
      )
    )

    state.nextPlayerIndex(0) shouldBe 0
  }

  it should "correctly identify when betting round is over" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000, currentBet = 20, hasActed = true, isActive = true),
        Player("p2", "Bob", 1000, currentBet = 20, hasActed = true, isActive = true)
      ),
      currentHighestBet = 20
    )

    state.isBettingRoundOver shouldBe true
  }

  it should "not be over when players haven't acted" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000, currentBet = 20, hasActed = false, isActive = true),
        Player("p2", "Bob", 1000, currentBet = 20, hasActed = true, isActive = true)
      ),
      currentHighestBet = 20
    )

    state.isBettingRoundOver shouldBe false
  }

  it should "be over when only one active player remains" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000, isActive = true, hasActed = true),
        Player("p2", "Bob", 1000, isActive = false, hasActed = true)
      ),
      currentHighestBet = 0
    )

    state.isBettingRoundOver shouldBe true
  }

  it should "reset bets and acts correctly" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000, currentBet = 50, hasActed = true),
        Player("p2", "Bob", 1000, currentBet = 50, hasActed = true)
      ),
      currentHighestBet = 50
    )

    val reset = state.resetBetsAndActs
    reset.players.foreach { p =>
      p.currentBet shouldBe 0
      p.hasActed shouldBe false
    }
    reset.currentHighestBet shouldBe 0
  }

  it should "collect bets into pot correctly" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 900, currentBet = 100),
        Player("p2", "Bob", 950, currentBet = 50)
      ),
      pot = Pot(Map("p1" -> 0, "p2" -> 0))
    )

    val collected = state.collectBets
    collected.pot.amount shouldBe 150
    collected.players.foreach(_.currentBet shouldBe 0)
  }

  it should "correctly identify current player" in {
    val state = GameState(
      id = "test",
      players = List(
        Player("p1", "Alice", 1000),
        Player("p2", "Bob", 1000)
      ),
      currentPlayerIndex = 1
    )

    state.currentPlayer.get.id shouldBe "p2"
  }

  it should "return None for current player if index out of bounds" in {
    val state = GameState(
      id = "test",
      players = List.empty,
      currentPlayerIndex = 0
    )

    state.currentPlayer shouldBe None
  }
}