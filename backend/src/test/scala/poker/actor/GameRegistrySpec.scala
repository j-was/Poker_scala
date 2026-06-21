package poker.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import poker.domain._

class GameRegistrySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "GameRegistry" should {
    "create games with unique codes" in {
      val registry = testKit.spawn(GameRegistry())
      val probe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), probe.ref)
      val result1 = probe.expectMessageType[GameRegistry.GameCreated]

      registry ! GameRegistry.CreateGame("player2", GameSettings(), probe.ref)
      val result2 = probe.expectMessageType[GameRegistry.GameCreated]

      result1.code should not be result2.code
    }

    "lookup existing game" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), createProbe.ref)
      val created = createProbe.expectMessageType[GameRegistry.GameCreated]

      val lookupProbe = testKit.createTestProbe[GameRegistry.LookupResult]()
      registry ! GameRegistry.LookupGame(created.code, lookupProbe.ref)
      lookupProbe.expectMessageType[GameRegistry.Found]
    }

    "return NotFound for non-existent game" in {
      val registry = testKit.spawn(GameRegistry())
      val probe = testKit.createTestProbe[GameRegistry.LookupResult]()

      registry ! GameRegistry.LookupGame("nonexistent", probe.ref)
      probe.expectMessage(GameRegistry.NotFound)
    }

    "remove game when actor stops" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), createProbe.ref)
      val created = createProbe.expectMessageType[GameRegistry.GameCreated]

      testKit.stop(created.ref)

      val lookupProbe = testKit.createTestProbe[GameRegistry.LookupResult]()
      registry ! GameRegistry.LookupGame(created.code, lookupProbe.ref)
      lookupProbe.expectMessage(GameRegistry.NotFound)
    }

    "list only public games" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(isPublic = true), createProbe.ref)
      val publicGame = createProbe.expectMessageType[GameRegistry.GameCreated]

      registry ! GameRegistry.CreateGame("player2", GameSettings(isPublic = false), createProbe.ref)
      createProbe.expectMessageType[GameRegistry.GameCreated]

      val listProbe = testKit.createTestProbe[GameRegistry.PublicGameListResult]()
      registry ! GameRegistry.ListPublicGames(listProbe.ref)
      val result = listProbe.expectMessageType[GameRegistry.PublicGameListResult]

      result.games.size shouldBe 1
      result.games.head.code shouldBe publicGame.code
    }

    "check if game name exists" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(name = "MyGame"), createProbe.ref)
      createProbe.expectMessageType[GameRegistry.GameCreated]

      val nameProbe = testKit.createTestProbe[Boolean]()
      registry ! GameRegistry.GameNameExists("MyGame", nameProbe.ref)
      nameProbe.expectMessage(true)

      registry ! GameRegistry.GameNameExists("OtherGame", nameProbe.ref)
      nameProbe.expectMessage(false)
    }
  }
}