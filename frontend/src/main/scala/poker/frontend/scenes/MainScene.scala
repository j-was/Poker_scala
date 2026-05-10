package poker.frontend.scenes

import poker.frontend.widgets.{CreateGameButton, JoinToGameButton, PokerScaleTitle}
import scalafx.geometry.Pos
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.layout.{BorderPane, VBox}

object MainScene {
  def apply() : Scene = {
    new Scene :
    {
      root = new BorderPane:
      {
        style = "-fx-background-color: #4f9a3a;"
        top = PokerScaleTitle.apply()
        center = new VBox:
        {
          alignment = Pos.Center
          spacing = 12
          children = Seq(
            CreateGameButton.apply(),
            JoinToGameButton.apply()
          )
        }
      }
    }
  }
}
