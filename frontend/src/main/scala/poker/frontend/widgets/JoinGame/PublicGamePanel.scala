package poker.frontend.widgets.JoinGame

import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label, ListCell, ListView, TextField}
import scalafx.scene.layout.{HBox, Priority, VBox}
import poker.frontend.widgets.Shared.JoinButton


case class RoomInfo(name: String, players: String, state: String, buyIn: String)

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

      val usernameRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Nazwa gracza:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 170
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz nazwę"
            prefWidth = 400
            style = "-fx-font-size: 20px;"
          }
        )
      }

      val searchRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Szukaj:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 170
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz nazwę pokoju"
            prefWidth = 400
            style = "-fx-font-size: 20px;"
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
        style = "-fx-font-size: 18px; -fx-background-color: transparent;"
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
                    style = "-fx-text-fill: #aaaaaa;"
                    prefWidth = 120
                  },
                  new Label(room.state) {
                    style = "-fx-text-fill: #aaaaaa;"
                    prefWidth = 260
                  },
                  new Label(room.buyIn) {
                    style = "-fx-text-fill: #ffd700;"
                    prefWidth = 160
                    alignment = Pos.CenterRight
                  }
                )
              }

              val bgColor = if (isSelected) "#4f9a3a" else "#3a3a3a"
              style = s"-fx-background-color: $bgColor; -fx-border-color: #2a2a2a; -fx-border-width: 0 0 2 0; -fx-padding: 10px;"
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

      children = Seq(usernameRow, searchRow, gamesList, buttonRow)
    }
  }
}