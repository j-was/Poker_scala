package poker.frontend.scenes

import poker.domain.ClientGameState
import poker.frontend.widgets.Game.{BottomPanel, InfoBar, TableArea}
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane

object Game {
  def apply(
     code: String,
     myPlayerId: String,
     state: ClientGameState,
     onFold: () => Unit = () => (),
     onCall: () => Unit = () => (),
     onCheck: () => Unit = () => (),
     onRaise: Int => Unit = _ => (),
     onStartGame: () => Unit = () => (),
  ) : Scene = {
    new Scene {
      stylesheets = Seq(new java.io.File("src/main/scala/poker/frontend/styles/game-scene.css").toURI.toString)

      root = new BorderPane {
        styleClass += "game-root"
        padding = Insets(20)

        top = InfoBar(code, state)
        center = TableArea(myPlayerId, state)
        bottom = BottomPanel(myPlayerId, state, onFold, onCall, onCheck, onRaise, onStartGame)
      }
    }
  }
}
