package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import poker.frontend.widgets.CreateGame.CreateGamePanel
import poker.frontend.widgets.Shared.ReturnButton

object CreateGame {
  def apply(): Scene = {
    new Scene {
      root = new BorderPane {
        style = "-fx-background-color: #4f9a3a;"
        padding = Insets(20)

        val topNavigation = new BorderPane {
          left = ReturnButton()
        }

        top = topNavigation

        center = new StackPane {
          padding = Insets(40, 150, 40, 150)
          children = Seq(CreateGamePanel())
        }
      }
    }
  }
}