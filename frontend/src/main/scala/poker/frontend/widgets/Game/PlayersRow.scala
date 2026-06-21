package poker.frontend.widgets.Game

import poker.domain.ClientGameState
import scalafx.geometry.Pos
import scalafx.scene.layout.FlowPane

object PlayersRow {
  def apply(myPlayerId: String, state: ClientGameState, currPlayerId: Option[String]) : FlowPane = {
    new FlowPane {
      alignment = Pos.Center
      hgap = 16
      vgap = 16

      children = state.players.zipWithIndex.map {case (player, index) =>
        PlayerSeat(player = player, isMe = player.id == myPlayerId, isDealer = index == state.dealerIndex,
          isCurrent = currPlayerId.contains(player.id))
      }
    }
  }
}
