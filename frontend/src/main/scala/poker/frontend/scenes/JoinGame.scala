package poker.frontend.scenes

import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.geometry.Insets
import scalafx.scene.control.ToggleGroup
import poker.frontend.widgets.JoinGame.{GameModeToggle, PrivateGamePanel, PublicGamePanel}
import poker.frontend.widgets.Shared.ReturnButton

object JoinGame {
  def apply(): Scene = {
    new Scene {
      root = new BorderPane {
        style = "-fx-background-color: #4f9a3a;"
        padding = Insets(20)

        val privatePanel = PrivateGamePanel()
        val publicPanel = PublicGamePanel()

        publicPanel.visible = false

        val modeGroup = new ToggleGroup()

        val topNavigation = new BorderPane {
          left = ReturnButton()
          right = GameModeToggle(
            modeGroup,
            () => {
              privatePanel.visible = true
              publicPanel.visible = false
            },
            () => {
              privatePanel.visible = false
              publicPanel.visible = true
            }
          )
        }

        top = topNavigation

        center = new StackPane {
          padding = Insets(40, 80, 40, 80)
          children = Seq(privatePanel, publicPanel)
        }
      }
    }
  }
}