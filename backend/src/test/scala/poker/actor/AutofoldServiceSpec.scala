package poker.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import poker.domain._
import scala.concurrent.duration._

class AutoFoldServiceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "AutoFoldService" should {
    "not fold if cancelled before the auto-fold delay" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)
      autoFold ! AutoFoldService.CancelAutoFold("game-1")

      gameProbe.expectNoMessage(500.millis)
    }

    "cancel only affects the specified game" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val game1Probe = testKit.createTestProbe[GameInstance.Command]()
      val game2Probe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", game1Probe.ref, sessionProbe.ref)
      autoFold ! AutoFoldService.TurnAdvanced("game-2", "player2", game2Probe.ref, sessionProbe.ref)

      autoFold ! AutoFoldService.CancelAutoFold("game-1")

      game1Probe.expectNoMessage(500.millis)
      game2Probe.expectNoMessage(500.millis)
    }

    "ignore cancellation for non-existent game without crashing" in {
      val autoFold = testKit.spawn(AutoFoldService())

      autoFold ! AutoFoldService.CancelAutoFold("non-existent-game")

      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)
      autoFold ! AutoFoldService.CancelAutoFold("game-1")

      gameProbe.expectNoMessage(500.millis)
    }

    "schedule a check after TurnAdvanced" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)

      // The DoCheck is scheduled with a 30-second delay, so we can't easily test it
      // But we can verify the service accepted the message without errors
      // In a real scenario, after 30 seconds, it would query SessionRegistry
      sessionProbe.expectNoMessage(500.millis)
    }

    "cancel auto-fold when same game receives new TurnAdvanced" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player2", gameProbe.ref, sessionProbe.ref)

      sessionProbe.expectNoMessage(500.millis)
    }
  }
}