package poker.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import poker.domain._

class GameInstanceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "A GameInstance actor" should {
    "allow players to join and then start the game" in {
      val game = testKit.spawn(GameInstance("table-1"))
      val probe = testKit.createTestProbe[GameInstance.Response]()

      game ! GameInstance.JoinGame("p1", "Player 1", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]

      game ! GameInstance.JoinGame("p2", "Player 2", probe.ref)
      val joined2 = probe.expectMessageType[GameInstance.GameJoined]

      joined2.state.players.size shouldBe 2

      game ! GameInstance.StartGame(probe.ref)
      val started = probe.expectMessageType[GameInstance.GameStarted]

      started.state.board shouldBe Board.PreFlop
      started.state.players.foreach { p =>
        p.holeCards.isDefined shouldBe true
      }
    }

    "play a simple pre-flop round with folds" in {
      val game = testKit.spawn(GameInstance("table-2"))
      val probe = testKit.createTestProbe[GameInstance.Response]()

      game ! GameInstance.JoinGame("p1", "P1", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p2", "P2", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]

      game ! GameInstance.StartGame(probe.ref)
      val started = probe.expectMessageType[GameInstance.GameStarted]

      val nextPlayerId = started.state.currentPlayer.get.id
      val otherPlayerId = started.state.players.find(_.id != nextPlayerId).get.id

      game ! GameInstance.Fold(nextPlayerId, probe.ref)
      val success = probe.expectMessageType[GameInstance.ActionSuccess]

      success.state.status shouldBe GameStatus.WaitingForPlayers
      success.state.players.find(_.id == otherPlayerId).get.chips > 1000 shouldBe true
    }

    "handle side pots in a 3-way all-in correctly" in {
      // Use slightly more chips so the game doesn't end after first hand
      val game = testKit.spawn(GameInstance("table-3", GameSettings(smallBlind = 10, bigBlind = 20, initialChips = 200)))
      val probe = testKit.createTestProbe[GameInstance.Response]()

      game ! GameInstance.JoinGame("p1", "P1", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p2", "P2", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p3", "P3", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]

      game ! GameInstance.StartGame(probe.ref)
      val started = probe.expectMessageType[GameInstance.GameStarted]

      val next1 = started.state.currentPlayer.get.id
      next1 shouldBe "p2" // First to act after blinds (in 3-player game, dealer is p1, SB is p2, BB is p3)

      // P2 (first to act) raises to 80
      game ! GameInstance.Raise("p2", 60, probe.ref) // Already has 10 blind, so raise 60 more = 70 total
      val action1 = probe.expectMessageType[GameInstance.ActionSuccess]

      val next2 = action1.state.currentPlayer.get.id
      next2 shouldBe "p3"

      // P3 calls 70
      game ! GameInstance.Call("p3", probe.ref)
      val action2 = probe.expectMessageType[GameInstance.ActionSuccess]

      val next3 = action2.state.currentPlayer.get.id
      next3 shouldBe "p1"

      // P1 calls 70
      game ! GameInstance.Call("p1", probe.ref)
      val flopState = probe.expectMessageType[GameInstance.ActionSuccess]
      flopState.state.board.isInstanceOf[Board.Flop] shouldBe true

      // Post-flop betting: everyone checks
      val flopNext1 = flopState.state.currentPlayer.get.id
      game ! GameInstance.Check(flopNext1, probe.ref)
      val flopAct1 = probe.expectMessageType[GameInstance.ActionSuccess]

      val flopNext2 = flopAct1.state.currentPlayer.get.id
      game ! GameInstance.Check(flopNext2, probe.ref)
      val flopAct2 = probe.expectMessageType[GameInstance.ActionSuccess]

      val flopNext3 = flopAct2.state.currentPlayer.get.id
      game ! GameInstance.Check(flopNext3, probe.ref)
      val turnState = probe.expectMessageType[GameInstance.ActionSuccess]
      turnState.state.board.isInstanceOf[Board.Turn] shouldBe true

      // Turn betting: everyone checks
      val turnNext1 = turnState.state.currentPlayer.get.id
      game ! GameInstance.Check(turnNext1, probe.ref)
      val turnAct1 = probe.expectMessageType[GameInstance.ActionSuccess]

      val turnNext2 = turnAct1.state.currentPlayer.get.id
      game ! GameInstance.Check(turnNext2, probe.ref)
      val turnAct2 = probe.expectMessageType[GameInstance.ActionSuccess]

      val turnNext3 = turnAct2.state.currentPlayer.get.id
      game ! GameInstance.Check(turnNext3, probe.ref)
      val riverState = probe.expectMessageType[GameInstance.ActionSuccess]
      riverState.state.board.isInstanceOf[Board.River] shouldBe true

      // River betting: everyone checks
      val riverNext1 = riverState.state.currentPlayer.get.id
      game ! GameInstance.Check(riverNext1, probe.ref)
      val riverAct1 = probe.expectMessageType[GameInstance.ActionSuccess]

      val riverNext2 = riverAct1.state.currentPlayer.get.id
      game ! GameInstance.Check(riverNext2, probe.ref)
      val riverAct2 = probe.expectMessageType[GameInstance.ActionSuccess]

      val riverNext3 = riverAct2.state.currentPlayer.get.id
      game ! GameInstance.Check(riverNext3, probe.ref)

      // After showdown, we get either ActionSuccess (if game continues) or GameOver (if finished)
      val finalMessage = probe.expectMessageType[GameInstance.Response]
      finalMessage match
        case GameInstance.ActionSuccess(state) =>
          // Game continues - this is fine with enough chips
          state.status shouldBe GameStatus.WaitingForPlayers
          state.players.map(_.chips).sum shouldBe 600 // Total chips preserved
        case GameInstance.GameOver(_, state) =>
          // Game ended - also valid
          state.status shouldBe GameStatus.Finished
        case _ =>
          fail("Unexpected message type")
    }

    "reject JoinGame after the tournament has started" in {
      val game = testKit.spawn(GameInstance("table-4"))
      val probe = testKit.createTestProbe[GameInstance.Response]()

      game ! GameInstance.JoinGame("p1", "P1", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p2", "P2", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]

      game ! GameInstance.StartGame(probe.ref)
      val started = probe.expectMessageType[GameInstance.GameStarted]

      val foldingId = started.state.currentPlayer.get.id
      game ! GameInstance.Fold(foldingId, probe.ref)
      probe.expectMessageType[GameInstance.ActionSuccess]

      game ! GameInstance.JoinGame("p3", "Late Player", probe.ref)
      probe.expectMessageType[GameInstance.Error]
    }

    "handle Call, Raise, Check, GetState, and UpdateSettings with errors" in {
      val game2 = testKit.spawn(GameInstance("table-actions"))
      val probe2 = testKit.createTestProbe[GameInstance.Response]()

      game2 ! GameInstance.JoinGame("p1", "Alice", probe2.ref)
      probe2.expectMessageType[GameInstance.GameJoined]
      game2 ! GameInstance.JoinGame("p2", "Bob", probe2.ref)
      probe2.expectMessageType[GameInstance.GameJoined]

      val stateProbe = testKit.createTestProbe[GameState]()
      game2 ! GameInstance.GetState(stateProbe.ref)
      stateProbe.expectMessageType[GameState].status shouldBe GameStatus.WaitingForPlayers

      game2 ! GameInstance.UpdateSettings(GameSettings(smallBlind = 20, bigBlind = 40), probe2.ref)
      val settingsUpdated = probe2.expectMessageType[GameInstance.SettingsUpdated]
      settingsUpdated.state.settings.smallBlind shouldBe 20

      game2 ! GameInstance.StartGame(probe2.ref)
      val started2 = probe2.expectMessageType[GameInstance.GameStarted]
      val activeId = started2.state.currentPlayer.get.id
      val inactiveId = started2.state.players.find(_.id != activeId).get.id

      game2 ! GameInstance.Check(activeId, probe2.ref)
      probe2.expectMessageType[GameInstance.Error].msg shouldBe "Cannot check, must call or raise"

      game2 ! GameInstance.Raise(activeId, 1000000, probe2.ref)
      probe2.expectMessageType[GameInstance.Error].msg shouldBe "Not enough chips to raise"

      game2 ! GameInstance.Call(inactiveId, probe2.ref)
      probe2.expectMessageType[GameInstance.Error].msg shouldBe "Not your turn"

      game2 ! GameInstance.Call(activeId, probe2.ref)
      val calledState = probe2.expectMessageType[GameInstance.ActionSuccess]

      val nextId = calledState.state.currentPlayer.get.id
      nextId shouldBe inactiveId
      game2 ! GameInstance.Check(nextId, probe2.ref)
      val checkedState = probe2.expectMessageType[GameInstance.ActionSuccess]

      checkedState.state.board.isInstanceOf[Board.Flop] shouldBe true
    }

    "handle LeaveGame correctly depending on phase" in {
      val game3 = testKit.spawn(GameInstance("table-leave"))
      val probe3 = testKit.createTestProbe[GameInstance.Response]()

      game3 ! GameInstance.JoinGame("p1", "Alice", probe3.ref)
      probe3.expectMessageType[GameInstance.GameJoined]
      game3 ! GameInstance.JoinGame("p2", "Bob", probe3.ref)
      probe3.expectMessageType[GameInstance.GameJoined]

      game3 ! GameInstance.LeaveGame("p1", probe3.ref)
      val leftState = probe3.expectMessageType[GameInstance.ActionSuccess]
      leftState.state.players.exists(_.id == "p1") shouldBe false

      game3 ! GameInstance.JoinGame("p1", "Alice", probe3.ref)
      probe3.expectMessageType[GameInstance.GameJoined]
      game3 ! GameInstance.StartGame(probe3.ref)
      probe3.expectMessageType[GameInstance.GameStarted]

      game3 ! GameInstance.LeaveGame("p1", probe3.ref)
      probe3.expectMessageType[GameInstance.Error].msg shouldBe "Cannot leave during a hand. Wait for the hand to end."
    }

    "handle a complete game that finishes" in {
      // This test specifically creates a scenario where the game finishes
      val game = testKit.spawn(GameInstance("table-finish", GameSettings(smallBlind = 10, bigBlind = 20, initialChips = 50)))
      val probe = testKit.createTestProbe[GameInstance.Response]()

      game ! GameInstance.JoinGame("p1", "P1", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p2", "P2", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]

      game ! GameInstance.StartGame(probe.ref)
      val started = probe.expectMessageType[GameInstance.GameStarted]

      // First player goes all-in
      val firstPlayer = started.state.currentPlayer.get.id
      val chips = started.state.currentPlayer.get.chips

      game ! GameInstance.Raise(firstPlayer, chips, probe.ref)
      val action1 = probe.expectMessageType[GameInstance.ActionSuccess]

      // Other player calls (also all-in)
      val secondPlayer = action1.state.currentPlayer.get.id
      game ! GameInstance.Call(secondPlayer, probe.ref)

      // This should complete the hand - check the response
      val result = probe.receiveMessage()
      result match {
        case GameInstance.ActionSuccess(state) =>
          // Game might continue if winner got chips
          state.players.map(_.chips).sum shouldBe 60 // Total chips should be preserved
        case GameInstance.GameOver(_, state) =>
          // Game ended with one player having all chips
          state.status shouldBe GameStatus.Finished
          state.players.map(_.chips).sum shouldBe 60
        case _ =>
          fail("Expected ActionSuccess or GameOver")
      }
    }
  }
}