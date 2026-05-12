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