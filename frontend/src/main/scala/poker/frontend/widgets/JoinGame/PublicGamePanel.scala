package poker.frontend.widgets.JoinGame

import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ListCell, ListView, TextField}
import scalafx.scene.layout.{HBox, Priority, VBox}
import poker.frontend.widgets.Shared.JoinButton
import scalafx.scene.effect.DropShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.paint.Color

case class RoomInfo(name: String, players: String, state: String, buyIn: String)

object PublicGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 20
      padding = Insets(40)
      style =
        """
          -fx-background-color: rgba(0, 0, 0, 0.85);
          -fx-background-radius: 20;
          -fx-border-color: #d4af37;
          -fx-border-width: 3;
          -fx-border-radius: 20;
        """

      val dollarsLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/dolar.png")) {
        fitWidth = 60
        preserveRatio = true
      }

      val titleLabel = new Label("Dołącz do Publicznej Gry") {
        style =
          """
            -fx-text-fill: white;
            -fx-font-size: 36px;
            -fx-font-weight: bold;
            -fx-font-family: "Noto Serif Display", serif;
          """
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

      val inputStyle =
        """
          -fx-font-size: 20px;
          -fx-background-color: rgba(255, 255, 255, 0.9);
          -fx-background-radius: 10;
          -fx-border-radius: 10;
          -fx-border-color: #d4af37;
          -fx-border-width: 2;
        """

      val usernameRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Nazwa gracza:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;"
            prefWidth = 170
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz nazwę"
            prefWidth = 400
            style = inputStyle
          }
        )
      }

      val searchRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Szukaj:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;"
            prefWidth = 170
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz nazwę pokoju"
            prefWidth = 400
            style = inputStyle
          }
        )
      }

      val mockData = ObservableBuffer(
        RoomInfo("Texas Hold'em Pro", "2/6 graczy", "Obecnie podczas rozgrywki", "5000$ wstępnego"),
        RoomInfo("Szybki Poker", "5/8 graczy", "Oczekuje na graczy", "100$ wstępnego"),
        RoomInfo("VIP Room", "4/6 graczy", "Obecnie podczas rozgrywki", "10000$ wstępnego")
      )

      val gamesList = new ListView[RoomInfo](mockData) {
        vgrow = Priority.Always
        style =
          """
            -fx-font-size: 18px;
            -fx-background-color: rgba(255, 255, 255, 0.1);
            -fx-control-inner-background: transparent;
            -fx-border-color: #d4af37;
            -fx-border-width: 2;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
          """
        cellFactory = (lv: ListView[RoomInfo]) => new ListCell[RoomInfo] {

          def updateVisuals(room: RoomInfo, isSelected: Boolean): Unit = {
            if (room != null) {
              graphic = new HBox {
                spacing = 20
                alignment = Pos.CenterLeft
                children = Seq(
                  new Label(room.name) {
                    style = "-fx-text-fill: white; -fx-font-weight: bold;"
                    prefWidth = 220
                  },
                  new Label(room.players) {
                    style = "-fx-text-fill: #cccccc;"
                    prefWidth = 120
                  },
                  new Label(room.state) {
                    style = "-fx-text-fill: #cccccc;"
                    prefWidth = 260
                  },
                  new Label(room.buyIn) {
                    style = "-fx-text-fill: #d4af37; -fx-font-weight: bold;"
                    prefWidth = 160
                    alignment = Pos.CenterRight
                  }
                )
              }

              val bgColor = if (isSelected) "rgba(212, 175, 55, 0.4)" else "transparent"
              style = s"-fx-background-color: $bgColor; -fx-border-color: rgba(212, 175, 55, 0.3); -fx-border-width: 0 0 1 0; -fx-padding: 15px;"
            } else {
              graphic = null
              style = "-fx-background-color: transparent; -fx-border-width: 0;"
            }
          }

          item.onChange { (_, _, room) =>
            updateVisuals(room, selected.value)
          }

          selected.onChange { (_, _, isSelected) =>
            updateVisuals(item.value, isSelected)
          }
        }
      }

      val buttonRow = new HBox {
        alignment = Pos.BottomRight
        children = Seq(
          JoinButton(() => {

          })
        )
      }

      children = Seq(titleRow, usernameRow, searchRow, gamesList, buttonRow)
    }
  }
}