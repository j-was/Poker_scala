package poker.domain

enum GameStatus:
  case WaitingForPlayers, Playing, Finished

case class GameSettings(
    smallBlind: Int = 10,
    bigBlind: Int = 20,
    initialChips: Int = 1000
)

case class Player(
    id: String,
    name: String,
    chips: Int,
    holeCards: Option[HoleCards] = None,
    currentBet: Int = 0,
    isActive: Boolean = true,
    hasActed: Boolean = false
)

case class Pot(contributions: Map[String, Int] = Map.empty):
  def amount: Int = contributions.values.sum
  def add(playerId: String, addedAmount: Int): Pot =
    Pot(contributions.updated(playerId, contributions.getOrElse(playerId, 0) + addedAmount))

case class GameState(
    id: String,
    status: GameStatus = GameStatus.WaitingForPlayers,
    settings: GameSettings = GameSettings(),
    board: Board = Board.PreFlop,
    players: List[Player] = List.empty,
    pot: Pot = Pot(),
    deck: Deck = Deck.full().shuffle,
    dealerIndex: Int = 0,
    currentPlayerIndex: Int = 0,
    currentHighestBet: Int = 0
):
  def activePlayers: List[Player] = players.filter(_.isActive)

  def currentPlayer: Option[Player] = 
    if players.indices.contains(currentPlayerIndex) then Some(players(currentPlayerIndex)) else None

  def nextPlayerIndex(currentIndex: Int): Int =
    if players.isEmpty then 0
    else
      var nextIdx = (currentIndex + 1) % players.size
      while !players(nextIdx).isActive && nextIdx != currentIndex do
        nextIdx = (nextIdx + 1) % players.size
      nextIdx

  def advanceTurn(): GameState =
    this.copy(currentPlayerIndex = nextPlayerIndex(currentPlayerIndex))

  /** Replaces the player with the same id in the players list. */
  def updatePlayer(player: Player): GameState =
    this.copy(players = players.map(p => if p.id == player.id then player else p))

  /** Returns true when all active players have acted and matched the highest bet.  
   *  Also returns true if only one player remains (everyone else folded). */
  def isBettingRoundOver: Boolean =
    val active = activePlayers
    if active.size <= 1 then true
    else active.forall(p => p.hasActed && p.currentBet == currentHighestBet)

  def resetBetsAndActs: GameState =
    this.copy(
      players = players.map(p => p.copy(currentBet = 0, hasActed = false)),
      currentHighestBet = 0
    )

  /** Moves all players' currentBets into the pot and resets bets/hasActed for the next street. */
  def collectBets: GameState =
    val newPot = players.foldLeft(pot) { (accPot, p) =>
      if p.currentBet > 0 then accPot.add(p.id, p.currentBet) else accPot
    }
    this.copy(
      pot = newPot,
      players = players.map(p => p.copy(currentBet = 0, hasActed = false)),
      currentHighestBet = 0
    )