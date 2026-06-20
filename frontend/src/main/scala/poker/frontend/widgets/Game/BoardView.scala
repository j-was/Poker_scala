package poker.frontend.widgets.Game

import poker.domain.Board
import scalafx.geometry.Pos
import scalafx.scene.layout.HBox

object BoardView {
  def apply(board: Board): HBox = {
    new HBox {
      alignment = Pos.Center
      spacing = 12

      val cards = board.cards

      children = if cards.isEmpty then Seq(GameLabel("Pre-Flop", 26, bold = true)) else cards.map(CardView.apply)
    }
  }
}
