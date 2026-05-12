package poker.frontend.widgets.JoinGame

import scalafx.scene.control.{RadioButton, ToggleGroup}
import scalafx.scene.layout.HBox

object GameModeToggle {
  def apply(modeGroup: ToggleGroup, onPrivate: () => Unit, onPublic: () => Unit): HBox = {
    val radioStyle = "-fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;"

    val privateBtn = new RadioButton("Prywatne") {
      toggleGroup = modeGroup
      selected = true
      style = radioStyle
      onAction = _ => onPrivate()
    }

    val publicBtn = new RadioButton("Publiczne") {
      toggleGroup = modeGroup
      style = radioStyle
      onAction = _ => onPublic()
    }

    new HBox {
      spacing = 20
      children = Seq(privateBtn, publicBtn)
    }
  }
}