package poker.frontend.widgets.Game

import poker.domain.ClientGameState
import scalafx.geometry.Pos
import scalafx.scene.layout.{BorderPane, VBox}

object TableArea {
  def apply (myPlayerId: String, state: ClientGameState): BorderPane = {
    val currPlayerId = state.players.lift(state.currentPlayerIndex).map(_.id)

    new BorderPane {
      center = new VBox {
        alignment = Pos.Center
        spacing = 24

        children = Seq(
          PlayersRow(myPlayerId, state, currPlayerId),
          BoardView(state.board),
          PotView(state)
        )
      }
    }
  }
}
