package poker.domain

import scala.util.Random

case class Deck(cards: List[Card]):
  def shuffle: Deck = Deck(Random.shuffle(cards))

  def draw(n: Int): (List[Card], Deck) = (cards.take(n), Deck(cards.drop(n)))

object Deck:
  def full(): Deck =
    val allCards = for
      suit <- Suit.values.toList
      rank <- Rank.values.toList
    yield Card(rank, suit)
    Deck(allCards)
