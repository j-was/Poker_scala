package poker.frontend.widgets.JoinGame

import scalafx.scene.control.{ToggleButton, ToggleGroup}
import scalafx.scene.layout.HBox

object GameModeToggle {
  def apply(modeGroup: ToggleGroup, onPrivate: () => Unit, onPublic: () => Unit): HBox = {
    val privateBtn = new ToggleButton("Prywatne") {
      toggleGroup = modeGroup
      selected = true
      style = "-fx-base: #2a2a2a; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;"
      onAction = _ => onPrivate()
    }

    val publicBtn = new ToggleButton("Publiczne") {
      toggleGroup = modeGroup
      style = "-fx-base: #2a2a2a; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;"
      onAction = _ => onPublic()
    }

    new HBox {
      spacing = 10
      children = Seq(privateBtn, publicBtn)
    }
  }
}