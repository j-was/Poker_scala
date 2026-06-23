package poker.frontend.scenes

import poker.domain.ClientGameState
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.VBox

object GameResult {
  def apply(code: String, winnerId: String, winnerName: String,
            state: ClientGameState, onReturnToLobby: () => Unit): Scene = {
    val winnerChips = state.players.find(_.id == winnerId).map(_.chips).getOrElse(0)

    new Scene {
      stylesheets = Seq(
        new java.io.File("src/main/scala/poker/frontend/styles/game-result-scene.css").toURI.toString
      )

      root = new VBox {
        styleClass += "game-result-scene"
        alignment = Pos.Center
        spacing = 24
        padding = Insets(40)

        children = Seq(
          new Label("Koniec gry") {
            styleClass += "game-result-title"
          },
          new Label(s"Zwycięzca: $winnerName") {
            styleClass += "game-result-winner"
          },
          new Label(s"Zdobył: $winnerChips żetonów") {
            styleClass += "game-result-chips"
          },
          new Label(s"Pokój: $code") {
            styleClass += "game-result-room-code"
          },
          new Button("Wróć do strony głównej") {
            styleClass += "game-result-button"
            onAction = _ => onReturnToLobby()
          }
        )
      }
    }
  }
}
