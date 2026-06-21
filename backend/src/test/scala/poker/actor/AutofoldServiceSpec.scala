package poker.actor

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import poker.domain._
import scala.concurrent.duration._

class AutoFoldServiceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "AutoFoldService" should {
    "not fold if cancelled before delay" in {
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
    }

    "ignore cancellation for non-existent game" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.CancelAutoFold("non-existent-game")

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)
      autoFold ! AutoFoldService.CancelAutoFold("game-1")

      gameProbe.expectNoMessage(500.millis)
    }

    "send fold command when player is disconnected" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)

      val isConnectedMsg = sessionProbe.expectMessageType[SessionRegistry.IsConnected]
      isConnectedMsg.playerId shouldBe "player1"

      isConnectedMsg.replyTo ! SessionRegistry.IsConnectedResult(false)

      val foldMsg = gameProbe.expectMessageType[GameInstance.Fold]
      foldMsg.playerId shouldBe "player1"
    }

    "not fold when player is connected" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)

      val isConnectedMsg = sessionProbe.expectMessageType[SessionRegistry.IsConnected]

      isConnectedMsg.replyTo ! SessionRegistry.IsConnectedResult(true)

      gameProbe.expectNoMessage(500.millis)
    }

    "cancel auto-fold when DiscardResponse is received" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)
    }

    "handle multiple turn advances for same game" in {
      val autoFold = testKit.spawn(AutoFoldService())
      val gameProbe = testKit.createTestProbe[GameInstance.Command]()
      val sessionProbe = testKit.createTestProbe[SessionRegistry.Command]()

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player1", gameProbe.ref, sessionProbe.ref)

      autoFold ! AutoFoldService.TurnAdvanced("game-1", "player2", gameProbe.ref, sessionProbe.ref)

      val isConnectedMsg = sessionProbe.expectMessageType[SessionRegistry.IsConnected]
      isConnectedMsg.playerId shouldBe "player2"

      isConnectedMsg.replyTo ! SessionRegistry.IsConnectedResult(false)

      val foldMsg = gameProbe.expectMessageType[GameInstance.Fold]
      foldMsg.playerId shouldBe "player2"
    }
  }
}