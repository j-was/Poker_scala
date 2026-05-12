package poker.frontend.widgets.MainSceneWidgets

import scalafx.scene.control.Button
import poker.frontend.ScenesNavigator

object CreateGameButton {
  def apply() : Button = {
    new Button("Utwórz grę"):
    {
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
      onAction = _ => ScenesNavigator.showCreateGame()
    }
  }
}
