package poker.frontend.widgets.Game

import poker.domain.ClientGameState
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.layout.VBox

object BottomPanel {
  def apply(myPlayerId: String, state: ClientGameState,
            onFold: () => Unit,
            onCall: () => Unit,
            onCheck: () => Unit,
            onRaise: Int => Unit,
            onStartGame: () => Unit
           ): VBox = {
    new VBox {
      spacing = 16
      padding = Insets(20, 0, 0, 0)
      alignment = Pos.Center

      children = Seq(
        MyHandPanel(state),
        ActionPanel(myPlayerId, state, onFold, onCall, onCheck, onRaise, onStartGame)
      )
    }
  }
}
