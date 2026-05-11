package poker.frontend.widgets.Shared

import scalafx.scene.control.Button
import poker.frontend.ScenesNavigator

object ReturnButton {
  def apply(): Button = {
    new Button("Wróć") {
      style =
        """
          -fx-background-color: transparent;
          -fx-text-fill: white;
          -fx-font-size: 24px;
          -fx-font-weight: bold;
          -fx-cursor: hand;
        """
      onAction = _ => ScenesNavigator.showMainStage()
    }
  }
}