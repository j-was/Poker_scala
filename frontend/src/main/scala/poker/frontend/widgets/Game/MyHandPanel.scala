package poker.frontend.widgets.Game

import poker.domain.ClientGameState
import scalafx.geometry.Pos
import scalafx.scene.layout.{HBox, VBox}

object MyHandPanel {
  def apply(state: ClientGameState): VBox = {
    val cards = state.myHoleCards.map(_.toList).getOrElse(Nil)

    new VBox {
      alignment = Pos.Center
      spacing = 10

      children = Seq(
        new HBox {
          alignment = Pos.Center
          spacing = 12
          children =
            if cards.isEmpty then Seq(GameLabel("Brak kart", 20))
            else cards.map(CardView.apply)
        },
        GameLabel(state.myHandCategory.map(category => s"Układ: $category").getOrElse("Układ: -"), 18, bold = true)
      )
    }
  }
}
