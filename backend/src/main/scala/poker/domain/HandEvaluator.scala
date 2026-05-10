package poker.domain

enum HandCategory(val strength: Int):
  case HighCard extends HandCategory(0)
  case OnePair extends HandCategory(1)
  case TwoPair extends HandCategory(2)
  case ThreeOfAKind extends HandCategory(3)
  case Straight extends HandCategory(4)
  case Flush extends HandCategory(5)
  case FullHouse extends HandCategory(6)
  case FourOfAKind extends HandCategory(7)
  case StraightFlush extends HandCategory(8)

/**
 * Final evaluated strength of a hand.
 * Comparable to determine the winner.
 *
 * @param category    The primary category (e.g. Flush, TwoPair)
 * @param tieBreakers The list of card ranks used to break ties (kickers).
 * @param bestCards   Cards that form this hand
 */
case class HandRank(category: HandCategory, tieBreakers: List[Int], bestCards: List[Card]) extends Ordered[HandRank]:
  override def compare(that: HandRank): Int =
    if this.category.strength != that.category.strength then
      this.category.strength - that.category.strength
    else
      val pairs = this.tieBreakers.zip(that.tieBreakers)
      pairs.find((a, b) => a != b) match
        case Some((a, b)) => a - b
        case None => 0

  override def equals(other: Any): Boolean = other match
    case that: HandRank => this.category == that.category && this.tieBreakers == that.tieBreakers
    case _ => false

  override def hashCode: Int = 31 * category.hashCode + tieBreakers.hashCode

object HandEvaluator:

  /**
   * Evaluates the best possible hand from the hole cards and whatever community
   * cards are currently on the board.
   */
  def evaluate(holeCards: HoleCards, board: Board): HandRank =
    val allCards = holeCards.toList ++ board.cards
    if allCards.size >= 5 then
      allCards.combinations(5).map(evaluateExact5).max
    else
      evaluateLessThan5(allCards)


  private def evaluateExact5(cards: List[Card]): HandRank =
    require(cards.size == 5, "exactly 5 cards are required to evaluate")

    val ranks = cards.map(_.rank.value).sorted(Ordering[Int].reverse)
    val isFlush = cards.map(_.suit).distinct.size == 1
    val isWheel = ranks == List(14, 5, 4, 3, 2)
    val isStraight = isWheel || (ranks.head - ranks.last == 4 && ranks.distinct.size == 5)
    val straightTieBreaker = if isWheel then 5 else ranks.head

    val rankCounts = ranks.groupBy(identity).view.mapValues(_.size).toList
      .sortBy { case (rank, count) => (-count, -rank) }

    if isFlush && isStraight then
      HandRank(HandCategory.StraightFlush, List(straightTieBreaker), cards)
    else rankCounts match
      case (quad, 4) :: (kicker, 1) :: Nil =>
        HandRank(HandCategory.FourOfAKind, List(quad, kicker), cards)

      case (trip, 3) :: (pair, 2) :: Nil =>
        HandRank(HandCategory.FullHouse, List(trip, pair), cards)

      case _ if isFlush =>
        HandRank(HandCategory.Flush, ranks, cards)

      case _ if isStraight =>
        HandRank(HandCategory.Straight, List(straightTieBreaker), cards)

      case (trip, 3) :: (k1, 1) :: (k2, 1) :: Nil =>
        HandRank(HandCategory.ThreeOfAKind, List(trip, k1, k2), cards)

      case (pair1, 2) :: (pair2, 2) :: (kicker, 1) :: Nil =>
        HandRank(HandCategory.TwoPair, List(pair1, pair2, kicker), cards)

      case (pair, 2) :: (k1, 1) :: (k2, 1) :: (k3, 1) :: Nil =>
        HandRank(HandCategory.OnePair, List(pair, k1, k2, k3), cards)

      case _ =>
        HandRank(HandCategory.HighCard, ranks, cards)

  private def evaluateLessThan5(cards: List[Card]): HandRank =
    val ranks = cards.map(_.rank.value).sorted(Ordering[Int].reverse)
    val rankCounts = ranks.groupBy(identity).view.mapValues(_.size).toList
      .sortBy { case (rank, count) => (-count, -rank) }

    rankCounts match
      case (quad, 4) :: _ =>
        HandRank(HandCategory.FourOfAKind, List(quad) ++ ranks.filter(_ != quad), cards)
      case (trip, 3) :: _ =>
        HandRank(HandCategory.ThreeOfAKind, List(trip) ++ ranks.filter(_ != trip), cards)
      case (pair1, 2) :: (pair2, 2) :: _ =>
        HandRank(HandCategory.TwoPair, List(pair1, pair2) ++ ranks.filter(r => r != pair1 && r != pair2), cards)
      case (pair, 2) :: _ =>
        HandRank(HandCategory.OnePair, List(pair) ++ ranks.filter(_ != pair), cards)
      case _ =>
        HandRank(HandCategory.HighCard, ranks, cards)
