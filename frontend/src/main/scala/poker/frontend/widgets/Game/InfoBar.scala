package poker.frontend.widgets.Game

import poker.domain.ClientGameState

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox


object InfoBar {
  def apply(code: String, state: ClientGameState): HBox = {
    new HBox {
      alignment = Pos.CenterLeft
      spacing = 24
      padding = Insets(0, 0, 20, 0)

      children = Seq(
        GameLabel(s"Pokój: $code", 20, bold = true),
        GameLabel(s"Status: ${state.status}", 18),
        GameLabel(s"Pula: ${state.pot}", 18),
        GameLabel(s"Najwyższy zakład: ${state.currentHighestBet}", 18),
      )
    }
  }
}
