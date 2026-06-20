package poker.frontend.widgets.MainSceneWidgets

import scalafx.scene.control.Button
import poker.frontend.ScenesNavigator

object JoinToGameButton {
  def apply() : Button = {
    new Button("Dołącz do gry") {
      prefWidth = 300
      prefHeight = 50
      styleClass += "button"
      onAction = _ => ScenesNavigator.showJoinGame()
    }
  }
}
