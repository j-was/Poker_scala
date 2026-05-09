package poker.domain

case class ClientPlayer(
    id: String,
    name: String,
    chips: Int,
    currentBet: Int,
    isActive: Boolean,
    hasActed: Boolean,
    hasCards: Boolean
)

case class ClientGameState(
    id: String,
    status: GameStatus,
    settings: GameSettings,
    board: Board,
    players: List[ClientPlayer],
    pot: Int,
    dealerIndex: Int,
    currentPlayerIndex: Int,
    currentHighestBet: Int,
    myHoleCards: Option[HoleCards],
    myHandCategory: Option[HandCategory],
    myBestCards: Option[List[Card]]
)

extension (state: GameState)
  /** Builds a safe, player-specific view of the game state.
   *  Other players' hole cards are hidden (only hasCards flag is exposed).
   *  The requesting player receives their own cards and current hand evaluation. */
  def toClientView(playerId: String): ClientGameState =
    val clientPlayers = state.players.map { p =>
      ClientPlayer(
        id = p.id,
        name = p.name,
        chips = p.chips,
        currentBet = p.currentBet,
        isActive = p.isActive,
        hasActed = p.hasActed,
        hasCards = p.holeCards.isDefined
      )
    }

    val myCards = state.players.find(_.id == playerId).flatMap(_.holeCards)
    val myRank = myCards.map(cards => HandEvaluator.evaluate(cards, state.board))

    ClientGameState(
      id = state.id,
      status = state.status,
      settings = state.settings,
      board = state.board,
      players = clientPlayers,
      pot = state.pot.amount,
      dealerIndex = state.dealerIndex,
      currentPlayerIndex = state.currentPlayerIndex,
      currentHighestBet = state.currentHighestBet,
      myHoleCards = myCards,
      myHandCategory = myRank.map(_.category),
      myBestCards = myRank.map(_.bestCards)
    )
