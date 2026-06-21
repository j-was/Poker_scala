package poker.frontend.widgets.WaitingRoom

import poker.frontend.ScenesNavigator
import scalafx.scene.control.Button

object ReadyButton {
  def apply(onClick: () => Unit): Button = {
    new Button("Rozpocznij") {
      styleClass += "ready-button"
      prefWidth = 200
      prefHeight = 50
      onAction = _ => onClick()
    }
  }
}