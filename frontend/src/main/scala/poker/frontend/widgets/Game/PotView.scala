package poker.frontend.widgets.Game

import poker.domain.ClientGameState
import scalafx.geometry.Pos
import scalafx.scene.layout.VBox

object PotView {
  def apply(state: ClientGameState): VBox = {
    new VBox {
      alignment = Pos.Center
      spacing = 6

      children = Seq(
        GameLabel(s"Pula: ${state.pot}", 24, bold = true),
        GameLabel(s"Small blind: ${state.settings.smallBlind}  Big blind: ${state.settings.bigBlind}", 16)
      )
    }
  }
}
