package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import poker.frontend.widgets.CreateGame.CreateGamePanel
import poker.frontend.widgets.Shared.ReturnButton

object CreateGame {
  def apply(): Scene = {
    new Scene {
      stylesheets = Seq(new java.io.File("src/main/scala/poker/frontend/styles/create-game-scene.css").toURI.toString)
      root = new StackPane {
        styleClass += "create-game-scene"

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