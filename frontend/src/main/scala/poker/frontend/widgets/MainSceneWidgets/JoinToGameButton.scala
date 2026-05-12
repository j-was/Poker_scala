package poker.frontend.widgets.MainSceneWidgets

import scalafx.scene.control.Button
import poker.frontend.ScenesNavigator

object JoinToGameButton {
  def apply() : Button = {
    new Button("Dołącz do gry") {
      prefWidth = 300
      prefHeight = 50
      style = """
           -fx-background-color: transparent;
           -fx-border-color: transparent;
           -fx-text-fill: white;
           -fx-font-size: 32px;
           -fx-font-weight: bold;
           -fx-cursor: hand;
        """
        onAction = _ => ScenesNavigator.showJoinGame()
    }
  }
}
