package poker.frontend.widgets.Game

import poker.domain.ClientPlayer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.layout.VBox

object PlayerSeat {
  def apply(player: ClientPlayer, isMe: Boolean, isDealer: Boolean, isCurrent: Boolean) : VBox = {
    val borderColor = if isCurrent then "#ffd54f" else if isMe then "#64b5f6" else "#1f1f1f"

    new VBox {
      alignment = Pos.Center
      spacing = 8
      padding = Insets(14)
      prefWidth = 190

      styleClass += "player-seat"
      style =
        s"""
           border-color: $borderColor
        """

      children = Seq(
        GameLabel(player.name + (if isMe then " (TY)" else "") + (if isDealer then " D" else ""), 18, bold = true),
        GameLabel(s"Żetony: ${player.chips}", 15),
        GameLabel(s"Zakład: ${player.currentBet}", 15),
        GameLabel(if player.isActive then "Aktywny" else "Pass", 15),
        HiddenCards(player.hasCards)
      )
    }
  }
}
