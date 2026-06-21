package poker.frontend.widgets.MainSceneWidgets

import scalafx.scene.control.Button
import poker.frontend.ScenesNavigator

object CreateGameButton {
  def apply() : Button = {
    new Button("Utwórz grę") {
      prefWidth = 300
      prefHeight = 50
      styleClass += "button"
      onAction = _ => ScenesNavigator.showCreateGame()
    }
  }
}
