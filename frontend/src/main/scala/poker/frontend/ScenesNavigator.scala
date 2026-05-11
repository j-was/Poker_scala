package poker.frontend

import poker.frontend.scenes.MainScene
import scalafx.application.JFXApp3

object ScenesNavigator
{
//  _ to deklaracja pusta, wstawia wartośc domyślną
  var mainStage: JFXApp3.PrimaryStage = _

  def showMainStage(): Unit =
  {
    mainStage.scene = MainScene()
  }
}