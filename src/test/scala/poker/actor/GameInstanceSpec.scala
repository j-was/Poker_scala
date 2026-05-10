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
      val game = testKit.spawn(GameInstance("table-3", GameSettings(smallBlind = 10, bigBlind = 20, initialChips = 100)))
      val probe = testKit.createTestProbe[GameInstance.Response]()

      game ! GameInstance.JoinGame("p1", "P1", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p2", "P2", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      game ! GameInstance.JoinGame("p3", "P3", probe.ref)
      probe.expectMessageType[GameInstance.GameJoined]
      
      game ! GameInstance.StartGame(probe.ref)
      val started = probe.expectMessageType[GameInstance.GameStarted]
      
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

      // A hand ends – one player folds
      val foldingId = started.state.currentPlayer.get.id
      game ! GameInstance.Fold(foldingId, probe.ref)
      probe.expectMessageType[GameInstance.ActionSuccess]

      // Now the game is between hands – new player tries to join
      game ! GameInstance.JoinGame("p3", "Late Player", probe.ref)
      probe.expectMessageType[GameInstance.Error]
    }
  }
}
