package poker.frontend.widgets

import scalafx.scene.control.Button


object JoinToGameButton {
  def apply() : Button = {
    new Button("Dołącz do gry"):
    {
      prefWidth = 300
      prefHeight = 50
      style =
        """
           -fx-background-color: transparent;
           -fx-border-color: transparent;
           -fx-text-fill: white;
           -fx-font-size: 32px;
           -fx-font-weight: bold;
           -fx-cursor: hand;
        """
    }
  }
}
