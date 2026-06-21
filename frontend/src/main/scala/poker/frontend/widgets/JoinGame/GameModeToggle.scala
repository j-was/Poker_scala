package poker.frontend.widgets.JoinGame

import scalafx.scene.control.{RadioButton, ToggleGroup}
import scalafx.scene.layout.HBox

object GameModeToggle {
  def apply(modeGroup: ToggleGroup, onPrivate: () => Unit, onPublic: () => Unit): HBox = {
    val privateBtn = new RadioButton("Prywatne") {
      toggleGroup = modeGroup
      selected = true
      styleClass += "game-mode-toggle"
      onAction = _ => onPrivate()
    }

    val publicBtn = new RadioButton("Publiczne") {
      toggleGroup = modeGroup
      styleClass += "game-mode-toggle"
      onAction = _ => onPublic()
    }

    new HBox {
      spacing = 25
      children = Seq(privateBtn, publicBtn)
    }
  }
}