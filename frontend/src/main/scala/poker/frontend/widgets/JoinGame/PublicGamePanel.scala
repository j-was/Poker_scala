package poker.frontend.widgets.JoinGame

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ListView, TextField}
import scalafx.scene.layout.{HBox, Priority, VBox}

object PublicGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 20
      padding = Insets(40)
      style =
        """
          -fx-background-color: #2a2a2a;
          -fx-background-radius: 20;
          -fx-border-color: #1a1a1a;
          -fx-border-width: 4;
          -fx-border-radius: 20;
        """

      val searchRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Szukaj:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
          },
          new TextField {
            promptText = "Wpisz nazwę pokoju"
            prefWidth = 400
            style = "-fx-font-size: 20px;"
          }
        )
      }

      val mockData = Seq(
        "Texas Hold'em Pro | 2/6 graczy | Obecnie podczas rozgrywki | 5000$ wstępnego",
        "Szybki Poker | 5/8 graczy | Oczekuje na graczy | 100$ wstępnego",
        "VIP Room | 4/6 graczy | Obecnie podczas rozgrywki | 10000$ wstępnego"
      )

      val gamesList = new ListView[String](mockData) {
        vgrow = Priority.Always
        style = "-fx-font-size: 18px;"
      }

      children = Seq(searchRow, gamesList)
    }
  }
}