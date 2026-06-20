package poker.frontend

import poker.domain.*
import poker.frontend.scenes.{CreateGame, GameScene, JoinGame, MainScene, WaitingRoom}
import scalafx.application.JFXApp3

object ScenesNavigator
{
  var mainStage: JFXApp3.PrimaryStage = _

  def showMainStage(): Unit =
  {
    mainStage.scene = MainScene()
  }
  def showJoinGame(): Unit =
    {
      mainStage.scene = JoinGame()
    }

  def showCreateGame(): Unit =
    {
      mainStage.scene = CreateGame()
    }

  def showWaitingRoom(): Unit =
    {
      mainStage.scene = WaitingRoom()
    }

  def showGameScene(
                     code: String,
                     myPlayerId: String,
                     state: ClientGameState
                   ): Unit = {
    mainStage.scene = GameScene(
      code = code,
      myPlayerId = myPlayerId,
      state = state,
      onFold = () => poker.frontend.client.PokerSession.fold(),
      onCall = () => poker.frontend.client.PokerSession.call(),
      onCheck = () => poker.frontend.client.PokerSession.check(),
      onRaise = amount => poker.frontend.client.PokerSession.raise(amount),
      onStartGame = () => poker.frontend.client.PokerSession.startGame(),
      onLeave = () => {
        poker.frontend.client.PokerSession.leaveGame()
        showMainStage()
      }
    )
  }
  
  def showServerState(code: String, myPlayerId: String, state: ClientGameState): Unit = {
    println(s"Status: ${state.status}")
    if state.status == poker.domain.GameStatus.WaitingForPlayers then
      mainStage.scene = WaitingRoom()
    else
      println("Ladowanie GameScene")
      showGameScene(code, myPlayerId, state)
  }

  def showMockGameScene(): Unit = {
    import poker.domain.*

    val state = ClientGameState(
      id = "test-room",
      status = GameStatus.Playing,
      settings = GameSettings(smallBlind = 10, bigBlind = 20, initialChips = 1000),
      board = Board.Flop(
        Card(Rank.Ace, Suit.Spades),
        Card(Rank.Ten, Suit.Hearts),
        Card(Rank.Two, Suit.Clubs)
      ),
      players = List(
        ClientPlayer("p1", "Alice", 980, 20, isActive = true, hasActed = true, hasCards = true),
        ClientPlayer("p2", "Bob", 960, 40, isActive = true, hasActed = false, hasCards = true),
        ClientPlayer("p3", "Carol", 1000, 0, isActive = false, hasActed = true, hasCards = false)
      ),
      pot = 90,
      dealerIndex = 0,
      currentPlayerIndex = 1,
      currentHighestBet = 40,
      myHoleCards = Some(HoleCards(
        Card(Rank.King, Suit.Diamonds),
        Card(Rank.King, Suit.Clubs)
      )),
      myHandCategory = Some(HandCategory.OnePair),
      myBestCards = None
    )

    showGameScene("test-room", "p1", state)
  }
}
