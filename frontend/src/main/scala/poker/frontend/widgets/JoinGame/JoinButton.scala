package poker.frontend.widgets.Shared

import scalafx.scene.control.Button

object JoinButton {
  def apply(onClick: () => Unit): Button = {
    new Button("Dołącz") {
      styleClass += "join-button"
      prefWidth = 150
      prefHeight = 50
      onAction = _ => onClick()
    }
  }
}