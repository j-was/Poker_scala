package poker.frontend.widgets.CreateGame

import scalafx.scene.control.Button

object CreateButton {
  def apply(onClick: () => Unit): Button = {
    new Button("Stwórz Grę") {
      styleClass += "create-button"
      prefWidth = 200
      prefHeight = 50
      onAction = _ => onClick()
    }
  }
}