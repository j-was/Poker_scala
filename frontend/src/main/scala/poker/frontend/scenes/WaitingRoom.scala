package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import poker.frontend.widgets.WaitingRoom.WaitingRoomPanel
import poker.frontend.widgets.Shared.ReturnButton

object WaitingRoom {
  def apply(): Scene = {
    new Scene {
      root = new StackPane {
        style = "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #2e7d32, #1b5e20);"

        val mainContent = new BorderPane {
          padding = Insets(20)

          top = new BorderPane {
            left = ReturnButton()
          }

          center = new StackPane {
            padding = Insets(40, 80, 40, 80)
            children = Seq(WaitingRoomPanel())
          }
        }

        children = Seq(mainContent)
      }
    }
  }
}