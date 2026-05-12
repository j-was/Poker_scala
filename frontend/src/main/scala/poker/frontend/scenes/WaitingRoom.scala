package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import poker.frontend.widgets.WaitingRoom.WaitingRoomPanel
import poker.frontend.widgets.Shared.ReturnButton

object WaitingRoom {
  def apply(): Scene = {
    new Scene {
      root = new BorderPane {
        style = "-fx-background-color: #4f9a3a;"
        padding = Insets(20)

        top = new BorderPane {
          left = ReturnButton()
        }

        center = new StackPane {
          padding = Insets(40, 80, 40, 80)
          children = Seq(WaitingRoomPanel())
        }
      }
    }
  }
}