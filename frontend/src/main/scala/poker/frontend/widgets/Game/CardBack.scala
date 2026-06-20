package poker.frontend.widgets.Game

import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane
import scalafx.scene.paint.Color

object CardBack {
  def apply(): StackPane = {
    new StackPane {
      prefWidth = 34
      prefHeight = 48
      maxWidth = 34
      maxHeight = 48
      styleClass += "card-back"
      children = Seq(
        new Label("◆") {
          textFill = Color.White
          styleClass += "card-back-text"
        }
      )
    }
  }
}
