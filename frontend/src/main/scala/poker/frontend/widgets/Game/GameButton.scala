package poker.frontend.widgets.Game

import scalafx.scene.control.Button

object GameButton {
  def apply(text: String, normalColor: String, hoverColor: String, onClick: () => Unit, ifDisabled: Boolean = false): Button = {
    new Button(text) {
      disable = ifDisabled
      withHoverStyle(this, normalColor, hoverColor)
      onAction = _ => onClick()
    }
  }
  private def withHoverStyle(button: Button, normalColor: String, hoverColor: String): Unit = {
    button.styleClass += "game-button"
    button.style =
      s"""
            -poker-button-color: $normalColor;
            -poker-button-hover-color: $hoverColor;
         """
  }
}
