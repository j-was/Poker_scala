package poker.frontend

import poker.frontend.scenes.MainScene
import scalafx.application.JFXApp3

object PokerApp extends JFXApp3:

  override def start(): Unit =
    stage = new JFXApp3.PrimaryStage:
    {
        title = "PokerScale"
        minWidth = 600
        minHeight = 400
        width = 1600
        height = 1000
        scene = MainScene.apply()
    }
