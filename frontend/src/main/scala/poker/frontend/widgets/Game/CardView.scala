package poker.frontend.widgets.Game

import poker.domain.{Card, Suit}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.StackPane
import scalafx.scene.paint.Color

object CardView {
  def apply(card: Card): StackPane = {
    val red = card.suit == Suit.Hearts || card.suit == Suit.Diamonds
    val textColor = if red then Color.DarkRed else Color.Black

    new StackPane {
      prefWidth = 72
      prefHeight = 104
      maxWidth = 72
      maxHeight = 104
      styleClass += "card"
      children = Seq(
        new Label(s"${card.rank}\n${card.suit}") {
          textFill = textColor
          styleClass += "card-top"
          StackPane.setAlignment(this, Pos.TopLeft)
          StackPane.setMargin(this, Insets(6, 0, 0, 7))
        },
        new Label(card.suit.toString) {
          textFill = textColor
          styleClass += "card-middle"
        },
        new Label(s"${card.rank}\n${card.suit}") {
          textFill = textColor
          styleClass += "card-bottom"
          StackPane.setAlignment(this, Pos.BottomRight)
          StackPane.setMargin(this, Insets(0, 7, 6, 0))
        }
      )
    }
  }
}
