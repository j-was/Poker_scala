package poker.frontend

import scalafx.application.JFXApp3
import poker.frontend.ScenesNavigator

object PokerApp extends JFXApp3 {

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage{
      title = "PokerScale"
      minWidth = 600
      minHeight = 400
      width = 1600
      height = 1000
    }
    ScenesNavigator.mainStage = stage
    ScenesNavigator.showMainStage()
  }
}
