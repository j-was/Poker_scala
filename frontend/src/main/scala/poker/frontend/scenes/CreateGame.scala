package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import poker.frontend.widgets.CreateGame.CreateGamePanel
import poker.frontend.widgets.Shared.ReturnButton

object CreateGame {
  def apply(): Scene = {
    new Scene {
      root = new StackPane {
        style = "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #2e7d32, #1b5e20);"

        val mainContent = new BorderPane {
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

        children = Seq(mainContent)
      }
    }
  }
}