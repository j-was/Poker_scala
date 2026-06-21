package poker.frontend.widgets.Game

import scalafx.geometry.Pos
import scalafx.scene.layout.HBox

object HiddenCards {
  def apply(hasCards: Boolean): HBox = {
    new HBox {
      alignment = Pos.Center
      spacing = 6

      children =
        if !hasCards then Seq(GameLabel("-", 16))
        else Seq(CardBack(), CardBack())
    }
  }
}
