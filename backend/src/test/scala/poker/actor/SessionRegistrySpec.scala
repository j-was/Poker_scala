package poker.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import poker.actor.SessionRegistry.JoinedGameResult
import poker.protocol.ServerMessage
import poker.domain.*

class SessionRegistrySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "SessionRegistry" should {
    "register and unregister sessions" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[ServerMessage]()

      registry ! SessionRegistry.Register("player1", probe.ref)

      val connProbe = testKit.createTestProbe[SessionRegistry.IsConnectedResult]()
      registry ! SessionRegistry.IsConnected("player1", connProbe.ref)
      connProbe.expectMessage(SessionRegistry.IsConnectedResult(true))

      registry ! SessionRegistry.Unregister("player1")
      registry ! SessionRegistry.IsConnected("player1", connProbe.ref)
      connProbe.expectMessage(SessionRegistry.IsConnectedResult(false))
    }

    "track joined games" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[JoinedGameResult]()

      registry ! SessionRegistry.JoinedGame("player1", "game-123")

      registry ! SessionRegistry.GetJoinedGame("player1", probe.ref)
      probe.expectMessage(SessionRegistry.JoinedGameResult(Some("game-123")))
    }

    "return None for unknown player's joined game" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[JoinedGameResult]()

      registry ! SessionRegistry.GetJoinedGame("unknown", probe.ref)
      probe.expectMessage(SessionRegistry.JoinedGameResult(None))
    }

    "handle LeftGame correctly" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[JoinedGameResult]()

      registry ! SessionRegistry.JoinedGame("player1", "game-123")
      registry ! SessionRegistry.LeftGame("player1", "game-123")

      registry ! SessionRegistry.GetJoinedGame("player1", probe.ref)
      probe.expectMessage(SessionRegistry.JoinedGameResult(None))
    }

    "send messages to specific player" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[ServerMessage]()

      registry ! SessionRegistry.Register("player1", probe.ref)
      registry ! SessionRegistry.SendTo("player1", ServerMessage.Pong)

      probe.expectMessage(ServerMessage.Pong)
    }

    "not fail when sending to unregistered player" in {
      val registry = testKit.spawn(SessionRegistry())

      // Should not throw or fail
      registry ! SessionRegistry.SendTo("unknown", ServerMessage.Pong)
    }

    "broadcast to game only to players in that game" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe1 = testKit.createTestProbe[ServerMessage]()
      val probe2 = testKit.createTestProbe[ServerMessage]()

      registry ! SessionRegistry.Register("player1", probe1.ref)
      registry ! SessionRegistry.Register("player2", probe2.ref)
      registry ! SessionRegistry.JoinedGame("player1", "game-A")
      registry ! SessionRegistry.JoinedGame("player2", "game-B")

      registry ! SessionRegistry.BroadcastRaw("game-A", ServerMessage.Pong)

      probe1.expectMessage(ServerMessage.Pong)
      probe2.expectNoMessage()
    }

    "broadcast with exception should skip specified player" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe1 = testKit.createTestProbe[ServerMessage]()
      val probe2 = testKit.createTestProbe[ServerMessage]()

      registry ! SessionRegistry.Register("player1", probe1.ref)
      registry ! SessionRegistry.Register("player2", probe2.ref)
      registry ! SessionRegistry.JoinedGame("player1", "game-A")
      registry ! SessionRegistry.JoinedGame("player2", "game-A")

      registry ! SessionRegistry.BroadcastRawExcept("game-A", "player1", ServerMessage.Pong)

      probe1.expectNoMessage()
      probe2.expectMessage(ServerMessage.Pong)
    }

    "broadcast state updates to game players" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[ServerMessage]()

      registry ! SessionRegistry.Register("player1", probe.ref)
      registry ! SessionRegistry.JoinedGame("player1", "game-A")

      val state = GameState(
        id = "game-A",
        players = List(Player("player1", "Alice", 1000))
      )

      registry ! SessionRegistry.BroadcastToGame("game-A", state, (pid, cs) =>
        ServerMessage.StateUpdate("game-A", cs)
      )

      probe.expectMessageType[ServerMessage.StateUpdate]
    }

    "clean up player associations when game is removed" in {
      val registry = testKit.spawn(SessionRegistry())
      val probe = testKit.createTestProbe[ServerMessage]()
      val joinedProbe = testKit.createTestProbe[SessionRegistry.JoinedGameResult]()

      registry ! SessionRegistry.Register("player1", probe.ref)
      registry ! SessionRegistry.Register("player2", probe.ref)

      registry ! SessionRegistry.JoinedGame("player1", "game-1")
      registry ! SessionRegistry.JoinedGame("player2", "game-1")

      registry ! SessionRegistry.GetJoinedGame("player1", joinedProbe.ref)
      joinedProbe.expectMessage(SessionRegistry.JoinedGameResult(Some("game-1")))

      registry ! SessionRegistry.GetJoinedGame("player2", joinedProbe.ref)
      joinedProbe.expectMessage(SessionRegistry.JoinedGameResult(Some("game-1")))

      registry ! SessionRegistry.GameRemoved("game-1")

      registry ! SessionRegistry.GetJoinedGame("player1", joinedProbe.ref)
      joinedProbe.expectMessage(SessionRegistry.JoinedGameResult(None))

      registry ! SessionRegistry.GetJoinedGame("player2", joinedProbe.ref)
      joinedProbe.expectMessage(SessionRegistry.JoinedGameResult(None))
    }
  }
}