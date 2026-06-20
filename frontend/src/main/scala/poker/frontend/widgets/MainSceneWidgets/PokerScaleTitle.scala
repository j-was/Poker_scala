package poker.frontend.widgets.MainSceneWidgets

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox

object PokerScaleTitle {
  def apply() : VBox = {
    new VBox:
    {
      alignment = Pos.Center
      padding = Insets(30, 0, 0, 0)

      children = Seq(
        new Label("Poker Scale"):
        {
          styleClass += "title"
        }
      )
    }
  }
}
