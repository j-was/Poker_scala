package poker.frontend.widgets

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
          style =
            """
              -fx-font-family: "Noto Serif Display", serif;
              -fx-font-size: 56px;
              -fx-font-weight: 700;
              -fx-text-fill: white;
            """
        }
      )
    }
  }
}
