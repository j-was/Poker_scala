package poker.frontend.widgets.JoinGame

import scalafx.scene.control.{RadioButton, ToggleGroup}
import scalafx.scene.layout.HBox

object GameModeToggle {
  def apply(modeGroup: ToggleGroup, isPublicInitial: Boolean, onPrivate: () => Unit, onPublic: () => Unit): HBox = {
    val privateBtn = new RadioButton("Prywatne") {
      toggleGroup = modeGroup
      selected = !isPublicInitial
      styleClass += "game-mode-toggle"
      onAction = _ => onPrivate()
    }

    val publicBtn = new RadioButton("Publiczne") {
      toggleGroup = modeGroup
      selected = isPublicInitial
      styleClass += "game-mode-toggle"
      onAction = _ => onPublic()
    }

    new HBox {
      spacing = 25
      children = Seq(privateBtn, publicBtn)
    }
  }
}