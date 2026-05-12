package poker.frontend.widgets.Shared

import scalafx.scene.control.Button

object JoinButton {
  def apply(onClick: () => Unit): Button = {
    new Button("Dołącz") {
      style = "-fx-background-color: #4f9a3a; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"
      prefWidth = 150
      prefHeight = 50
//      onAction = _ =>
    }
  }
}