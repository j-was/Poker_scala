package poker.domain

enum GameStatus:
  case WaitingForPlayers, Playing, Finished

case class GameSettings(
                         smallBlind: Int = 10,
                         bigBlind: Int = 20,
                         initialChips: Int = 1000
                       )

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
