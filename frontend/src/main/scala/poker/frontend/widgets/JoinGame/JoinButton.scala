package poker.frontend.widgets.Shared

import scalafx.scene.control.Button

object JoinButton {
  def apply(onClick: () => Unit): Button = {
    val baseStyle =
      """
         -fx-background-color: rgba(0, 0, 0, 0.3);
         -fx-border-color: #d4af37;
         -fx-border-width: 2px;
         -fx-border-radius: 30px;
         -fx-background-radius: 30px;
         -fx-text-fill: white;
         -fx-font-size: 20px;
         -fx-font-weight: bold;
         -fx-cursor: hand;
      """

    val hoverStyle =
      """
         -fx-background-color: rgba(212, 175, 55, 0.4);
         -fx-border-color: #d4af37;
         -fx-border-width: 2px;
         -fx-border-radius: 30px;
         -fx-background-radius: 30px;
         -fx-text-fill: white;
         -fx-font-size: 20px;
         -fx-font-weight: bold;
         -fx-cursor: hand;
      """

    val btn = new Button("Dołącz") {
      style = baseStyle
      prefWidth = 150
      prefHeight = 50
      onAction = _ => onClick()
    }

    btn.onMouseEntered = _ => btn.style = hoverStyle
    btn.onMouseExited = _ => btn.style = baseStyle

    btn
  }
}