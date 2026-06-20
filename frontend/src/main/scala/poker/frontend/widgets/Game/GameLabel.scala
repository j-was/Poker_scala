package poker.frontend.widgets.Game

import scalafx.scene.control.Label
import scalafx.scene.paint.Color

object GameLabel {
  def apply(textValue: String, size: Int, bold: Boolean = false): Label = {
    new Label(textValue) {
      textFill = Color.White
      style =
        s"""
           -fx-font-size: ${size}px;
           ${if bold then "-fx-font-weight: bold;" else ""}
        """
    }
  }
}
