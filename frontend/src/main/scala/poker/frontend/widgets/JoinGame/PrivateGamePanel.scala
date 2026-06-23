package poker.frontend.widgets.JoinGame

import poker.frontend.ScenesNavigator
import poker.frontend.client.PokerSession
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, TextField}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.Shared.{JoinButton, ErrorDialog}
import scalafx.scene.effect.DropShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.paint.Color

object PrivateGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 30
      padding = Insets(50)
      styleClass += "private-game-panel"

      val dollarsLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/dolar.png")) {
        fitWidth = 60
        preserveRatio = true
      }

      val titleLabel = new Label("Dołącz do Prywatnej Gry") {
        styleClass += "private-game-panel-title"
        effect = new DropShadow {
          color = Color.Black
          radius = 10
          offsetX = 3
          offsetY = 3
        }
      }

      val dollarsRight = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/dolar.png")) {
        fitWidth = 60
        preserveRatio = true
      }

      val titleRow = new HBox {
        alignment = Pos.Center
        spacing = 20
        children = Seq(dollarsLeft, titleLabel, dollarsRight)
      }

      val usernameField = new TextField {
        text = PokerSession.currentPlayerName
        editable = !PokerSession.isPlayerNameLocked
        promptText = "Wpisz nazwę"
        prefWidth = 320
        styleClass += "private-game-panel-input"
      }

      val usernameRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("Nazwa gracza:") {
            styleClass += "private-game-panel-row-label"
            prefWidth = 180
            alignment = Pos.CenterRight
          },
          usernameField
        )
      }

      val codeField = new TextField {
        promptText = "Wpisz ID pokoju"
        prefWidth = 320
        styleClass += "private-game-panel-input"
      }

      val idRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("ID Pokoju:") {
            styleClass += "private-game-panel-row-label"
            prefWidth = 180
            alignment = Pos.CenterRight
          },
          codeField
        )
      }

//      val passwordRow = new HBox {
//        alignment = Pos.CenterRight
//        spacing = 15
//        maxWidth = 500
//        children = Seq(
//          new Label("Hasło:") {
//            style = "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;"
//            prefWidth = 180
//            alignment = Pos.CenterRight
//          },
//          new TextField {
//            promptText = "Wpisz hasło"
//            prefWidth = 320
//            style = inputStyle
//          }
//        )
//      }

      val spacer = new Region {
        vgrow = Priority.Always
      }

      val buttonRow = new HBox {
        alignment = Pos.BottomRight
        children = Seq(
          JoinButton(() => {
            PokerSession.configure(
              name = usernameField.text.value,
              stateHandler = ScenesNavigator.showServerState,
              errorHandler = msg => ErrorDialog.show(msg)
            )

            PokerSession.joinGame(codeField.text.value)
          })
        )
      }

      children = Seq(titleRow, usernameRow, idRow, spacer, buttonRow)
    }
  }
}