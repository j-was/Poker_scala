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
      result1.code should fullyMatch regex """[a-z]+-[a-z]+-\d{3}"""
    }

    "lookup existing game" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), createProbe.ref)
      val created = createProbe.expectMessageType[GameRegistry.GameCreated]

      val lookupProbe = testKit.createTestProbe[GameRegistry.LookupResult]()
      registry ! GameRegistry.LookupGame(created.code, lookupProbe.ref)

      val found = lookupProbe.expectMessageType[GameRegistry.Found]
      found.code shouldBe created.code
    }

    "return NotFound for non-existent game" in {
      val registry = testKit.spawn(GameRegistry())
      val probe = testKit.createTestProbe[GameRegistry.LookupResult]()

      registry ! GameRegistry.LookupGame("nonexistent-game-999", probe.ref)
      probe.expectMessage(GameRegistry.NotFound)
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
      result.games.head.isPublic shouldBe true
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

    "create game with auto-generated name when name is empty" in {
      val registry = testKit.spawn(GameRegistry())
      val probe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(name = ""), probe.ref)
      val result = probe.expectMessageType[GameRegistry.GameCreated]

      result.code should fullyMatch regex """[a-z]+-[a-z]+-\d{3}"""
    }

    "return empty list when no public games exist" in {
      val registry = testKit.spawn(GameRegistry())
      val listProbe = testKit.createTestProbe[GameRegistry.PublicGameListResult]()

      registry ! GameRegistry.ListPublicGames(listProbe.ref)

      val result = listProbe.expectMessageType[GameRegistry.PublicGameListResult]
      result.games shouldBe empty
    }

    "lookup by game code should be case-sensitive" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), createProbe.ref)
      val created = createProbe.expectMessageType[GameRegistry.GameCreated]

      // Lookup with different case should fail
      val lookupProbe = testKit.createTestProbe[GameRegistry.LookupResult]()
      registry ! GameRegistry.LookupGame(created.code.toUpperCase, lookupProbe.ref)
      lookupProbe.expectMessage(GameRegistry.NotFound)
    }

    "auto-dissolve empty game when host creates new game" in {
      val registry = testKit.spawn(GameRegistry())
      val probe = testKit.createTestProbe[GameRegistry.CreateResult]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), probe.ref)
      val result1 = probe.expectMessageType[GameRegistry.GameCreated]

      registry ! GameRegistry.CreateGame("player1", GameSettings(), probe.ref)
      val result2 = probe.expectMessageType[GameRegistry.GameCreated]

      val lookupProbe = testKit.createTestProbe[GameRegistry.LookupResult]()
      registry ! GameRegistry.LookupGame(result1.code, lookupProbe.ref)
      lookupProbe.expectMessage(GameRegistry.NotFound)

      registry ! GameRegistry.LookupGame(result2.code, lookupProbe.ref)
      lookupProbe.expectMessageType[GameRegistry.Found]
    }

    "check if player has a game" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()
      val hasGameProbe = testKit.createTestProbe[Boolean]()

      registry ! GameRegistry.PlayerHasGame("player1", hasGameProbe.ref)
      hasGameProbe.expectMessage(false)

      registry ! GameRegistry.CreateGame("player1", GameSettings(), createProbe.ref)
      createProbe.expectMessageType[GameRegistry.GameCreated]

      registry ! GameRegistry.PlayerHasGame("player1", hasGameProbe.ref)
      hasGameProbe.expectMessage(true)
    }

    "remove game and clean up host tracking" in {
      val registry = testKit.spawn(GameRegistry())
      val createProbe = testKit.createTestProbe[GameRegistry.CreateResult]()
      val hasGameProbe = testKit.createTestProbe[Boolean]()

      registry ! GameRegistry.CreateGame("player1", GameSettings(), createProbe.ref)
      val created = createProbe.expectMessageType[GameRegistry.GameCreated]

      registry ! GameRegistry.PlayerHasGame("player1", hasGameProbe.ref)
      hasGameProbe.expectMessage(true)

      registry ! GameRegistry.RemoveGame(created.code)

      val lookupProbe = testKit.createTestProbe[GameRegistry.LookupResult]()
      registry ! GameRegistry.LookupGame(created.code, lookupProbe.ref)
      lookupProbe.expectMessage(GameRegistry.NotFound)

      registry ! GameRegistry.PlayerHasGame("player1", hasGameProbe.ref)
      hasGameProbe.expectMessage(false)
    }
  }
}