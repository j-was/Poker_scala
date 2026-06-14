package poker.frontend.widgets.JoinGame

import poker.frontend.ScenesNavigator
import poker.frontend.client.PokerSession
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.Shared.JoinButton

object PrivateGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 30
      padding = Insets(50)
      style =
        """
          -fx-background-color: #2a2a2a;
          -fx-background-radius: 20;
          -fx-border-color: #1a1a1a;
          -fx-border-width: 4;
          -fx-border-radius: 20;
        """

      val usernameField = new TextField {
        promptText = "Wpisz nazwę"
        prefWidth = 320
        style = "-fx-font-size: 20px;"
      }

      val usernameRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("Nazwa gracza:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 180
            alignment = Pos.CenterRight
          },
          usernameField
        )
      }

      val codeField = new TextField {
        promptText = "Wpisz ID pokoju"
        prefWidth = 320
        style = "-fx-font-size: 20px;"
      }

      val idRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("ID Pokoju:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 180
            alignment = Pos.CenterRight
          },
          codeField
        )
      }

      val passwordRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("Hasło:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 180
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz hasło"
            prefWidth = 320
            style = "-fx-font-size: 20px;"
          }
        )
      }

      val spacer = new Region {
        vgrow = Priority.Always
      }

      val buttonRow = new HBox {
        alignment = Pos.BottomRight
        children = Seq(
          JoinButton(() => {
            PokerSession.configure(
              name = usernameField.text.value,
              stateHandler = ScenesNavigator.showServerState
            )

            PokerSession.joinGame(codeField.text.value)
          })
        )
      }

      children = Seq(usernameRow, idRow, passwordRow, spacer, buttonRow)
    }
  }
}