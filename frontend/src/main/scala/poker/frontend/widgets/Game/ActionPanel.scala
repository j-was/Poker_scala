package poker.frontend.widgets.Game

import poker.domain.{ClientGameState, GameStatus}
import scalafx.geometry.Pos
import scalafx.scene.control.TextField
import scalafx.scene.layout.HBox

object ActionPanel {
  def apply(
             myPlayerId: String,
             state: ClientGameState,
             onFold: () => Unit,
             onCall: () => Unit,
             onCheck: () => Unit,
             onRaise: Int => Unit,
             onStartGame: () => Unit
           ): HBox = {
    val isMyTurn =
      state.status == GameStatus.Playing &&
        state.players.lift(state.currentPlayerIndex).exists(_.id == myPlayerId)

    val raiseInput = new TextField {
      promptText = "Raise"
      prefWidth = 100
      disable = !isMyTurn
      styleClass += "raise-input"
    }

    new HBox {
      alignment = Pos.Center
      spacing = 12

      children = Seq(
        GameButton("Fold", "#7a2d2d", "#8f3939", onFold, !isMyTurn),
        GameButton("Check", "#2f5f8f", "#3b70a5", onCheck, !isMyTurn),
        GameButton("Call", "#2f7a4f", "#3a8d5e", onCall, !isMyTurn),
        raiseInput,
        GameButton("Raise", "#8a6a20", "#9f7b2a", () => {
          raiseInput.text.value.toIntOption.foreach(onRaise)
        }, !isMyTurn),
        GameButton("Start", "#4f7f3f", "#5f934c", onStartGame, state.status == GameStatus.Playing)
      )
    }
  }
}
