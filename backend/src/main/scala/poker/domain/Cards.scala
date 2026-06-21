package poker.domain

enum Suit:
  case Clubs, Diamonds, Hearts, Spades

  override def toString: String = this match
    case Clubs => "♣"
    case Diamonds => "♦"
    case Hearts => "♥"
    case Spades => "♠"

enum Rank(val value: Int):
  case Two extends Rank(2)
  case Three extends Rank(3)
  case Four extends Rank(4)
  case Five extends Rank(5)
  case Six extends Rank(6)
  case Seven extends Rank(7)
  case Eight extends Rank(8)
  case Nine extends Rank(9)
  case Ten extends Rank(10)
  case Jack extends Rank(11)
  case Queen extends Rank(12)
  case King extends Rank(13)
  case Ace extends Rank(14)

  override def toString: String = this match
    case Ten => "T"
    case Jack => "J"
    case Queen => "Q"
    case King => "K"
    case Ace => "A"
    case other => other.value.toString

case class Card(rank: Rank, suit: Suit):
  override def toString: String = s"$rank$suit"

case class HoleCards(c1: Card, c2: Card):
  def toList: List[Card] = List(c1, c2)

  override def toString: String = s"[$c1, $c2]"

enum Board:
  case PreFlop
  case Flop(c1: Card, c2: Card, c3: Card)
  case Turn(c1: Card, c2: Card, c3: Card, c4: Card)
  case River(c1: Card, c2: Card, c3: Card, c4: Card, c5: Card)

  def cards: List[Card] = this match
    case PreFlop => Nil
    case Flop(c1, c2, c3) => List(c1, c2, c3)
    case Turn(c1, c2, c3, c4) => List(c1, c2, c3, c4)
    case River(c1, c2, c3, c4, c5) => List(c1, c2, c3, c4, c5)

  override def toString: String = this match
    case PreFlop => "Pre-Flop"
    case Flop(c1, c2, c3) => s"Flop: [$c1, $c2, $c3]"
    case Turn(c1, c2, c3, c4) => s"Turn: [$c1, $c2, $c3, $c4]"
    case River(c1, c2, c3, c4, c5) => s"River: [$c1, $c2, $c3, $c4, $c5]"
