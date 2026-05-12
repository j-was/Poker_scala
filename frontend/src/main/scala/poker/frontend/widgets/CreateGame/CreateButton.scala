package poker.frontend.widgets.CreateGame

import scalafx.scene.control.Button

object CreateButton {
  def apply(onClick: () => Unit): Button = {
    new Button("Stwórz Grę") {
      style = "-fx-background-color: #4f9a3a; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"
      prefWidth = 200
      prefHeight = 50
      onAction = _ => onClick()
    }
  }
}