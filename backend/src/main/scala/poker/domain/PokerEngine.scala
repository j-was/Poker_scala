package poker.domain

object PokerEngine:

  /** Starts a new hand: shuffles deck, deals 2 cards to each player with chips,
   *  posts blinds, and sets the first player to act. Players with 0 chips sit out. */
  def startNewHand(state: GameState): GameState =
    if state.players.size < 2 then
      return state.copy(status = GameStatus.Finished)

    val deck1 = Deck.full().shuffle
    var currentDeck = deck1
    
    val playersWithCards = state.players.map { p =>
      if p.chips > 0 then
        val (cards, newDeck) = currentDeck.draw(2)
        currentDeck = newDeck
        p.copy(holeCards = Some(HoleCards(cards(0), cards(1))), isActive = true, hasActed = false, currentBet = 0)
      else
        p.copy(isActive = false, hasActed = true)
    }

    val activeCount = playersWithCards.count(_.isActive)
    if activeCount < 2 then
      return state.copy(status = GameStatus.Finished)

    val dealerIdx = (state.dealerIndex + 1) % playersWithCards.size
    
    val activeIndices = playersWithCards.indices.filter(i => playersWithCards(i).isActive)
    val sortedActive = activeIndices.sortBy(i => if i >= dealerIdx then i - dealerIdx else i + playersWithCards.size)
    
    val sbIdx = if activeCount == 2 then dealerIdx else sortedActive(1)
    val bbIdx = if activeCount == 2 then sortedActive(1) else sortedActive(2)

    val sbAmount = math.min(state.settings.smallBlind, playersWithCards(sbIdx).chips)
    val bbAmount = math.min(state.settings.bigBlind, playersWithCards(bbIdx).chips)

    val updatedPlayers = playersWithCards.updated(sbIdx, playersWithCards(sbIdx).copy(
      chips = playersWithCards(sbIdx).chips - sbAmount,
      currentBet = sbAmount
    )).updated(bbIdx, playersWithCards(bbIdx).copy(
      chips = playersWithCards(bbIdx).chips - bbAmount,
      currentBet = bbAmount
    ))

    val nextToActIdx = if activeCount == 2 then dealerIdx else sortedActive(3 % activeCount)

    state.copy(
      status = GameStatus.Playing,
      board = Board.PreFlop,
      players = updatedPlayers,
      deck = currentDeck,
      dealerIndex = dealerIdx,
      currentPlayerIndex = nextToActIdx,
      pot = Pot(Map.empty),
      currentHighestBet = bbAmount
    )

  /** Moves the game to the next street (PreFlop → Flop → Turn → River → Showdown).
   *  Collects all bets into the pot first. If only one player remains active, awards
   *  the pot immediately without going to showdown. */
  def advancePhase(state: GameState): GameState =
    val stateWithCollectedBets = state.collectBets
    val activeCount = stateWithCollectedBets.activePlayers.size

    if activeCount == 1 then
      distributePotAndReset(stateWithCollectedBets)
    else
      stateWithCollectedBets.board match
        case Board.PreFlop =>
          val (cards, deck) = stateWithCollectedBets.deck.draw(3)
          stateWithCollectedBets.copy(
            board = Board.Flop(cards(0), cards(1), cards(2)),
            deck = deck,
            currentPlayerIndex = stateWithCollectedBets.nextPlayerIndex(stateWithCollectedBets.dealerIndex)
          )
        case Board.Flop(c1, c2, c3) =>
          val (cards, deck) = stateWithCollectedBets.deck.draw(1)
          stateWithCollectedBets.copy(
            board = Board.Turn(c1, c2, c3, cards(0)),
            deck = deck,
            currentPlayerIndex = stateWithCollectedBets.nextPlayerIndex(stateWithCollectedBets.dealerIndex)
          )
        case Board.Turn(c1, c2, c3, c4) =>
          val (cards, deck) = stateWithCollectedBets.deck.draw(1)
          stateWithCollectedBets.copy(
            board = Board.River(c1, c2, c3, c4, cards(0)),
            deck = deck,
            currentPlayerIndex = stateWithCollectedBets.nextPlayerIndex(stateWithCollectedBets.dealerIndex)
          )
        case Board.River(c1, c2, c3, c4, c5) =>
          distributePotAndReset(stateWithCollectedBets)

  /** Evaluates all active players' hands, handles side pots for all-in situations,
   *  distributes chips to winner(s), then resets the table to WaitingForPlayers.
   *  Sets status to Finished if only one player has chips left. */
  def distributePotAndReset(state: GameState): GameState =
    val activePlayers = state.activePlayers
    
    var contributions = state.pot.contributions
    var playersState = state.players.map(p => p.id -> p).toMap
    
    val tiers: List[List[String]] = if activePlayers.size == 1 then
      List(List(activePlayers.head.id))
    else
      val hands = activePlayers.map(p => p.id -> HandEvaluator.evaluate(p.holeCards.get, state.board)).toMap
      hands.toList.groupBy(_._2).toList.sortBy(_._1).reverse.map(_._2.map(_._1))

    for (winners <- tiers if contributions.values.sum > 0) do
      var currentWinners = winners.filter(w => contributions.getOrElse(w, 0) > 0)
      
      while currentWinners.nonEmpty && contributions.values.sum > 0 do
        val minContrib = currentWinners.map(contributions).min
        val takeAmounts = contributions.view.mapValues(c => math.min(c, minContrib)).toMap
        val subPot = takeAmounts.values.sum
        contributions = contributions.map { case (pId, c) => pId -> (c - takeAmounts(pId)) }
        
        val winAmount = subPot / currentWinners.size
        val extra = subPot % currentWinners.size
        
        currentWinners.zipWithIndex.foreach { case (wId, idx) =>
          val p = playersState(wId)
          val bonus = if idx == 0 then extra else 0
          playersState = playersState.updated(wId, p.copy(chips = p.chips + winAmount + bonus))
        }
        
        currentWinners = currentWinners.filter(w => contributions.getOrElse(w, 0) > 0)

    val totalRemaining = contributions.values.sum
    if totalRemaining > 0 then
      val absoluteWinnerId = tiers.head.head
      val p = playersState(absoluteWinnerId)
      playersState = playersState.updated(absoluteWinnerId, p.copy(chips = p.chips + totalRemaining))

    val finalPlayers = state.players.map(p => playersState(p.id).copy(
      holeCards = None,
      isActive = true,
      hasActed = false,
      currentBet = 0
    ))

    val finalState = state.copy(
      status = GameStatus.WaitingForPlayers,
      pot = Pot(Map.empty),
      players = finalPlayers,
      board = Board.PreFlop
    )
    
    if finalPlayers.count(_.chips > 0) <= 1 then
      finalState.copy(status = GameStatus.Finished)
    else
      finalState
