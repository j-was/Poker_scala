package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import poker.frontend.widgets.WaitingRoom.WaitingRoomPanel
import poker.frontend.widgets.Shared.ReturnButton
import poker.domain.ClientGameState

object WaitingRoom {
  def apply(code: String, myPlayerId: String, state: ClientGameState): Scene = {
    new Scene {
      stylesheets = Seq(new java.io.File("src/main/scala/poker/frontend/styles/waiting-room-scene.css").toURI.toString)
      root = new StackPane {
        styleClass += "waiting-room-scene"

        val mainContent = new BorderPane {
          padding = Insets(20)

          top = new BorderPane {
            left = ReturnButton()
          }

          center = new StackPane {
            id = "waiting-room-center"
            padding = Insets(40, 80, 40, 80)
            children = Seq(WaitingRoomPanel(code, myPlayerId, state))
          }
        }

        children = Seq(mainContent)
      }
    }
  }
}