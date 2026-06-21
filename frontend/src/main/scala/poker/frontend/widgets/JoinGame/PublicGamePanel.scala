package poker.frontend.widgets.JoinGame

import poker.frontend.ScenesNavigator
import poker.frontend.client.PokerSession
import poker.frontend.widgets.Shared.ErrorDialog
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ListCell, ListView, TextField, Button}
import scalafx.scene.layout.{HBox, Priority, VBox}
import poker.frontend.widgets.Shared.JoinButton
import scalafx.scene.effect.DropShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.paint.Color

case class RoomInfo(code: String, name: String, players: String, state: String, buyIn: String)

object PublicGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 20
      padding = Insets(40)
      styleClass += "public-game-panel"

      val dollarsLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/dolar.png")) {
        fitWidth = 60
        preserveRatio = true
      }

      val titleLabel = new Label("Dołącz do Publicznej Gry") {
        styleClass += "public-game-panel-title"
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
        promptText = "Wpisz nazwę"
        prefWidth = 400
        styleClass += "public-game-panel-input"
      }

      val usernameRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Nazwa gracza:") {
            styleClass += "public-game-panel-row-label"
            prefWidth = 170
            alignment = Pos.CenterRight
          },
          usernameField
        )
      }

      val searchField = new TextField {
        promptText = "Wpisz nazwę pokoju"
        prefWidth = 400
        styleClass += "public-game-panel-input"
      }

      val gamesData = ObservableBuffer[RoomInfo]()

      val refreshButton = new Button("Odśwież") {
        styleClass += "public-game-panel-input"
        onAction = _ => {
          val playerName = usernameField.text.value
          if (playerName.trim.nonEmpty) {
            PokerSession.listPublicGames(
              playerName,
              games => {
                gamesData.clear()
                val filterText = searchField.text.value.trim.toLowerCase
                val filtered = if (filterText.isEmpty) games else games.filter(_.name.toLowerCase.contains(filterText))
                filtered.foreach { g =>
                  gamesData += RoomInfo(
                    g.code,
                    g.name,
                    s"${g.playerCount}/${g.maxPlayers}",
                    "Aktywna",
                    s"Blindy: ${g.smallBlind}/${g.bigBlind}"
                  )
                }
              },
              msg => ErrorDialog.show(msg)
            )
          } else {
            ErrorDialog.show("Wpisz nazwę gracza przed pobraniem listy!")
          }
        }
      }

      val searchRow = new HBox {
        alignment = Pos.Center
        spacing = 15
        children = Seq(
          new Label("Szukaj:") {
            styleClass += "public-game-panel-row-label"
            prefWidth = 170
            alignment = Pos.CenterRight
          },
          searchField,
          refreshButton
        )
      }

      val gamesList = new ListView[RoomInfo](gamesData) {
        vgrow = Priority.Always
        styleClass += "private-game-panel-games-list"
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
        spacing = 15
        children = Seq(
          JoinButton(() => {
            val selectedRoom = gamesList.selectionModel.value.getSelectedItem
            val playerName = usernameField.text.value

            if (selectedRoom != null && playerName.trim.nonEmpty) {
              PokerSession.joinGame(selectedRoom.code)
            }
          })
        )
      }

      children = Seq(titleRow, usernameRow, searchRow, gamesList, buttonRow)
    }
  }
}