package poker.frontend.widgets.JoinGame

import scalafx.scene.control.{ToggleButton, ToggleGroup}
import scalafx.scene.layout.HBox

object GameModeToggle {
  def apply(modeGroup: ToggleGroup, onPrivate: () => Unit, onPublic: () => Unit): HBox = {
    val styleBase = "-fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand; -fx-border-color: #1a1a1a; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;"
    val styleSelected = styleBase + " -fx-background-color: #4f9a3a;"
    val styleUnselected = styleBase + " -fx-background-color: #2a2a2a;"

    val privateBtn = new ToggleButton("Prywatne") {
      toggleGroup = modeGroup
      selected = true
      style = styleSelected
    }

    val publicBtn = new ToggleButton("Publiczne") {
      toggleGroup = modeGroup
      style = styleUnselected
    }

    privateBtn.selected.onChange { (_, _, isSelected) =>
      privateBtn.style = if (isSelected) styleSelected else styleUnselected
    }

    publicBtn.selected.onChange { (_, _, isSelected) =>
      publicBtn.style = if (isSelected) styleSelected else styleUnselected
    }

    privateBtn.onAction = _ => onPrivate()
    publicBtn.onAction = _ => onPublic()

    new HBox {
      spacing = 10
      children = Seq(privateBtn, publicBtn)
    }
  }
}