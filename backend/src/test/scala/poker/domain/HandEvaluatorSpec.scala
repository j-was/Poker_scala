package poker.domain

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

def determineWinners(playersCards: Map[String, HoleCards], board: Board): List[String] =
  val evaluations = playersCards.map { case (id, hc) => id -> HandEvaluator.evaluate(hc, board) }
  if evaluations.isEmpty then List.empty
  else
    val bestRank = evaluations.values.max
    evaluations.filter { case (_, rank) => rank == bestRank }.keys.toList

class HandEvaluatorSpec extends AnyFlatSpec with Matchers {

  "HandEvaluator" should "evaluate High Card and resolve ties by kickers" in {
    val p1 = HoleCards(Card(Rank.King, Suit.Spades), Card(Rank.Ten, Suit.Hearts))
    val p2 = HoleCards(Card(Rank.King, Suit.Clubs), Card(Rank.Nine, Suit.Diamonds))
    val board = Board.River(
      Card(Rank.Seven, Suit.Spades), Card(Rank.Five, Suit.Hearts), Card(Rank.Four, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Clubs)
    )
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.HighCard)
    r1.bestCards.size.shouldBe(5)

    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.shouldBe(List("1"))
  }

  it should "evaluate One Pair and resolve ties by pair rank, then kickers" in {
    val p1 = HoleCards(Card(Rank.Jack, Suit.Spades), Card(Rank.Ten, Suit.Hearts))
    val p2 = HoleCards(Card(Rank.Jack, Suit.Clubs), Card(Rank.Nine, Suit.Diamonds))
    val board = Board.River(
      Card(Rank.Jack, Suit.Hearts), Card(Rank.Five, Suit.Hearts), Card(Rank.Four, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Clubs)
    )
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.OnePair)
    r1.tieBreakers.shouldBe(List(11, 10, 5, 4))
    r1.bestCards.size.shouldBe(5)

    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.shouldBe(List("1"))
  }

  it should "evaluate Two Pair and resolve ties" in {
    // Both have Jacks and Fives. p1 has Ace kicker, p2 has King kicker.
    val p1 = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ten, Suit.Hearts))
    val p2 = HoleCards(Card(Rank.King, Suit.Clubs), Card(Rank.Nine, Suit.Diamonds))
    val board = Board.River(
      Card(Rank.Jack, Suit.Hearts), Card(Rank.Jack, Suit.Spades), Card(Rank.Five, Suit.Diamonds),
      Card(Rank.Five, Suit.Hearts), Card(Rank.Three, Suit.Clubs)
    )
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.TwoPair)
    r1.tieBreakers.shouldBe(List(11, 5, 14)) // Jacks, Fives, Ace kicker
    r1.bestCards.size.shouldBe(5)

    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.shouldBe(List("1"))
  }

  it should "evaluate Three of a Kind" in {
    val p1 = HoleCards(Card(Rank.Seven, Suit.Spades), Card(Rank.Seven, Suit.Hearts))
    val board = Board.River(
      Card(Rank.Seven, Suit.Diamonds), Card(Rank.Five, Suit.Hearts), Card(Rank.Four, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Clubs)
    )
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.ThreeOfAKind)
    r1.tieBreakers.shouldBe(List(7, 5, 4)) // 7s, kickers 5 and 4
    r1.bestCards.size.shouldBe(5)
  }

  it should "evaluate Straight and resolve higher straight" in {
    val p1 = HoleCards(Card(Rank.Eight, Suit.Spades), Card(Rank.Four, Suit.Hearts)) // 4,5,6,7,8
    val p2 = HoleCards(Card(Rank.Nine, Suit.Clubs), Card(Rank.Eight, Suit.Diamonds)) // 5,6,7,8,9
    val board = Board.River(
      Card(Rank.Five, Suit.Diamonds), Card(Rank.Six, Suit.Hearts), Card(Rank.Seven, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Two, Suit.Clubs)
    )
    
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.Straight)
    r1.tieBreakers.shouldBe(List(8))
    r1.bestCards.size.shouldBe(5)
    
    val r2 = HandEvaluator.evaluate(p2, board)
    r2.category.shouldBe(HandCategory.Straight)
    r2.tieBreakers.shouldBe(List(9))

    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.shouldBe(List("2"))
  }

  it should "evaluate Wheel Straight (A-2-3-4-5)" in {
    val p1 = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Two, Suit.Hearts))
    val board = Board.River(
      Card(Rank.Three, Suit.Diamonds), Card(Rank.Four, Suit.Hearts), Card(Rank.Five, Suit.Diamonds),
      Card(Rank.Nine, Suit.Hearts), Card(Rank.Ten, Suit.Clubs)
    )
    
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.Straight)
    r1.tieBreakers.shouldBe(List(5)) // High card of the straight is 5
    r1.bestCards.size.shouldBe(5)
  }

  it should "evaluate Flush and resolve higher flush" in {
    val p1 = HoleCards(Card(Rank.King, Suit.Spades), Card(Rank.Two, Suit.Spades))
    val p2 = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Three, Suit.Spades))
    val board = Board.River(
      Card(Rank.Jack, Suit.Spades), Card(Rank.Ten, Suit.Spades), Card(Rank.Four, Suit.Spades),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Clubs)
    )
    // p1 flush: K, J, T, 4, 2
    // p2 flush: A, J, T, 4, 3
    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.Flush)
    r1.bestCards.size.shouldBe(5)
    
    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.shouldBe(List("2"))
  }

  it should "evaluate Full House and resolve correctly" in {
    val p1 = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)) // Kings full of Aces
    val p2 = HoleCards(Card(Rank.Queen, Suit.Spades), Card(Rank.Queen, Suit.Hearts)) // Kings full of Queens
    val board = Board.River(
      Card(Rank.King, Suit.Spades), Card(Rank.King, Suit.Hearts), Card(Rank.King, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Diamonds)
    )

    val r1 = HandEvaluator.evaluate(p1, board)
    r1.category.shouldBe(HandCategory.FullHouse)
    r1.tieBreakers.shouldBe(List(13, 14)) // Trip Kings, Pair of Aces
    r1.bestCards.size.shouldBe(5)

    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.shouldBe(List("1"))
  }

  it should "evaluate Four of a Kind" in {
    val hole = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Spades))
    val board = Board.River(
      Card(Rank.Ace, Suit.Hearts), Card(Rank.Ace, Suit.Clubs), Card(Rank.Ace, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Diamonds)
    )
    val r = HandEvaluator.evaluate(hole, board)
    r.category.shouldBe(HandCategory.FourOfAKind)
    r.tieBreakers.shouldBe(List(14, 13)) // Quads of Aces, King kicker
    r.bestCards.size.shouldBe(5)
  }

  it should "evaluate Straight Flush (Royal Flush)" in {
    val hole = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Spades))
    val board = Board.River(
      Card(Rank.Queen, Suit.Spades), Card(Rank.Jack, Suit.Spades), Card(Rank.Ten, Suit.Spades),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Diamonds)
    )
    
    val rank = HandEvaluator.evaluate(hole, board)
    rank.category.shouldBe(HandCategory.StraightFlush)
    rank.tieBreakers.shouldBe(List(14))
    rank.bestCards.size.shouldBe(5)
  }

  it should "handle a split pot (tie)" in {
    val p1 = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.King, Suit.Hearts))
    val p2 = HoleCards(Card(Rank.Ace, Suit.Clubs), Card(Rank.King, Suit.Diamonds))
    val board = Board.River(
      Card(Rank.Queen, Suit.Spades), Card(Rank.Jack, Suit.Hearts), Card(Rank.Ten, Suit.Diamonds),
      Card(Rank.Two, Suit.Hearts), Card(Rank.Three, Suit.Diamonds)
    )

    val winners = determineWinners(Map("1" -> p1, "2" -> p2), board)
    winners.toSet.shouldBe(Set("1", "2"))
  }

  it should "evaluate current hand on PreFlop (less than 5 cards)" in {
    val hole = HoleCards(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts))
    val board = Board.PreFlop
    val r = HandEvaluator.evaluate(hole, board)
    r.category.shouldBe(HandCategory.OnePair)
    r.bestCards.shouldBe(List(Card(Rank.Ace, Suit.Spades), Card(Rank.Ace, Suit.Hearts)))
  }

  it should "correctly implement equals and hashCode in HandRank" in {
    val r1 = HandRank(HandCategory.OnePair, List(14, 10), List(Card(Rank.Ace, Suit.Spades)))
    val r2 = HandRank(HandCategory.OnePair, List(14, 10), List(Card(Rank.Ace, Suit.Hearts)))
    val r3 = HandRank(HandCategory.TwoPair, List(14, 10), List(Card(Rank.Ace, Suit.Spades)))

    r1 shouldBe r2
    r1.hashCode shouldBe r2.hashCode

    r1 shouldNot be (r3)
    r1.equals("not a hand rank") shouldBe false
  }
}

