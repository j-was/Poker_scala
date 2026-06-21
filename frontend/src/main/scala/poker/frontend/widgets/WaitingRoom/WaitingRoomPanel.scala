package poker.frontend.widgets.WaitingRoom

import poker.frontend.client.PokerSession
import poker.domain.GameSettings
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label, ListCell, ListView, Slider, TextField, ToggleGroup}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.JoinGame.GameModeToggle
import scalafx.scene.effect.DropShadow
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.paint.Color

case class Player(nick: String, isReady: Boolean)

object WaitingRoomPanel {
  def apply(): VBox = {
    val current = PokerSession.currentView
    val code = current.map(_._1).getOrElse("-")
    val myPlayerId = current.map(_._2).getOrElse("")
    val state = current.map(_._3)
    val currentSettings = state.map(_.settings).getOrElse(GameSettings())

    val isOwner = state.exists(_.players.headOption.exists(_.id == myPlayerId))

    new VBox {
      alignment = Pos.TopCenter
      spacing = 30
      padding = Insets(40)
      styleClass += "waiting-room-panel"

      val dollarsLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/dolar.png")) {
        fitWidth = 60
        preserveRatio = true
      }

      val titleLabel = new Label("Poczekalnia") {
        styleClass += "waiting-room-panel-title"
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

      val roomInfoRow = new HBox {
        alignment = Pos.Center
        spacing = 40
        styleClass += "waiting-room-panel-room-info-row"
        children = Seq(
          createHeaderInfo("ID Pokoju:", code),
          createHeaderInfo("Typ:", if (currentSettings.isPublic) "Publiczny" else "Prywatny")
        )
      }

      val contentRow = new HBox {
        spacing = 40
        vgrow = Priority.Always

        val playersColumn = new VBox {
          spacing = 10
          hgrow = Priority.Always

          val playersCountLabel = new Label(s"Gracze (${state.map(_.players.size).getOrElse(0)}/${currentSettings.maxPlayers})") {
            style = "-fx-text-fill: #d4af37; -fx-font-size: 20px; -fx-font-weight: bold;"
          }

          val playersList = new ListView[Player](
            ObservableBuffer.from(
              state.toList.flatMap(_.players).map { p =>
                Player(p.name + (if (p.id == myPlayerId) " (Ty)" else ""), true)
              }
            )
          ) {
            vgrow = Priority.Always
            styleClass += "waiting-room-panel-players-list"
            cellFactory = (lv: ListView[Player]) => new ListCell[Player] {
              item.onChange { (_, _, p) =>
                if (p != null) {
                  graphic = new HBox {
                    alignment = Pos.CenterLeft
                    spacing = 15
                    children = Seq(
                      new Label(if (p.isReady) "✔" else "✘") {
                        style = s"-fx-text-fill: ${if (p.isReady) "#d4af37" else "#ff4444"}; -fx-font-size: 20px; -fx-font-weight: bold;"
                      },
                      new Label(p.nick) {
                        style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
                      }
                    )
                  }
                  style = "-fx-background-color: transparent; -fx-border-color: rgba(212, 175, 55, 0.3); -fx-border-width: 0 0 1 0; -fx-padding: 10px;"
                } else {
                  graphic = null
                  style = "-fx-background-color: transparent; -fx-border-width: 0;"
                }
              }
            }
          }
          children = Seq(playersCountLabel, playersList)
        }

        def createSliderRow(labelStr: String, minVal: Int, maxVal: Int, initialVal: Int): (HBox, Slider) = {
          val valueLabel = new Label(labelStr) {
            styleClass += "waiting-room-panel-slider-value"
            prefWidth = 90
          }

          val textField = new TextField {
            text = initialVal.toString
            prefWidth = 65
            styleClass += "waiting-room-panel-slider-text-field"
          }

          val slider = new Slider {
            min = minVal.toDouble
            max = maxVal.toDouble
            value = initialVal.toDouble
            hgrow = Priority.Always
            showTickMarks = false
            showTickLabels = false
            styleClass += "waiting-room-panel-slider"
          }

          slider.value.onChange { (_, _, newVal) =>
            val displayVal = newVal.intValue().toString
            if (!textField.focused.value) textField.text = displayVal
          }

          textField.text.onChange { (_, _, newVal) =>
            try {
              val d = newVal.toInt
              if (d >= minVal && d <= maxVal) slider.value = d.toDouble
            } catch {
              case _: Exception =>
            }
          }

          val row = new HBox {
            alignment = Pos.CenterLeft
            spacing = 10
            children = Seq(valueLabel, slider, textField)
          }

          (row, slider)
        }

        val settingsColumn = new VBox {
          spacing = 20
          prefWidth = 350
          alignment = Pos.TopLeft
          styleClass += "waiting-room-panel-settings"

          if (isOwner) {
            val header = new Label("EDYCJA USTAWIEŃ") {
              style = "-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 20px;"
            }

            val roomNameField = new TextField {
              promptText = "Nazwa pokoju"
              text = currentSettings.name
              styleClass += "waiting-room-panel-new-room"
            }

            var isPublicMode = currentSettings.isPublic
            val modeGroup = new ToggleGroup()
            val modeToggle = GameModeToggle(
              modeGroup,
              () => isPublicMode = false,
              () => isPublicMode = true
            )

            val (maxPlayersRow, maxPlayersSlider) = createSliderRow("Max graczy:", 2, 10, currentSettings.maxPlayers)
            val (buyInRow, buyInSlider) = createSliderRow("Wpisowe ($):", 100, 10000, currentSettings.initialChips)
            val (smallBlindRow, smallBlindSlider) = createSliderRow("Small Blind:", 5, 500, currentSettings.smallBlind)
            val (bigBlindRow, bigBlindSlider) = createSliderRow("Big Blind:", 10, 1000, currentSettings.bigBlind)

            smallBlindSlider.value.onChange { (_, _, newVal) =>
              if (bigBlindSlider.value.value < newVal.doubleValue) {
                bigBlindSlider.value = newVal.doubleValue
              }
            }

            bigBlindSlider.value.onChange { (_, _, newVal) =>
              if (smallBlindSlider.value.value > newVal.doubleValue) {
                smallBlindSlider.value = newVal.doubleValue
              }
              if (buyInSlider.value.value < newVal.doubleValue) {
                buyInSlider.value = newVal.doubleValue
              }
            }

            buyInSlider.value.onChange { (_, _, newVal) =>
              if (bigBlindSlider.value.value > newVal.doubleValue) {
                bigBlindSlider.value = newVal.doubleValue
              }
            }

            val acceptButton = new Button("Akceptuj") {
              style = "-fx-background-color: rgba(212, 175, 55, 0.8); -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;"
              prefWidth = 350
              onAction = _ => {
                PokerSession.updateSettings(GameSettings(
                  name = roomNameField.text.value,
                  smallBlind = smallBlindSlider.value.value.toInt,
                  bigBlind = bigBlindSlider.value.value.toInt,
                  initialChips = buyInSlider.value.value.toInt,
                  isPublic = isPublicMode,
                  maxPlayers = maxPlayersSlider.value.value.toInt
                ))
              }
            }

            children = Seq(header, roomNameField, modeToggle, maxPlayersRow, buyInRow, smallBlindRow, bigBlindRow, acceptButton)
          } else {
            children = Seq(
              new Label("USTAWIENIA") {
                style = "-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 20px;"
              },
              createSettingRow("Wpisowe:", s"${currentSettings.initialChips}$$"),
              createSettingRow("Small Blind:", s"${currentSettings.smallBlind}$$"),
              createSettingRow("Big Blind:", s"${currentSettings.bigBlind}$$")
            )
          }
        }

        children = Seq(playersColumn, settingsColumn)
      }

      val spacer = new Region {
        vgrow = Priority.Always
      }
      val footer = new HBox {
        alignment = Pos.BottomRight
        children = Seq(ReadyButton(() => PokerSession.startGame()))
      }

      children = Seq(titleRow, roomInfoRow, contentRow, spacer, footer)
    }
  }

  private def createHeaderInfo(label: String, value: String): VBox = new VBox {
    alignment = Pos.Center
    children = Seq(
      new Label(label) { style = "-fx-text-fill: #cccccc; -fx-font-size: 14px;" },
      new Label(value) { style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;" }
    )
  }

  private def createSettingRow(label: String, value: String): VBox = new VBox {
    spacing = 5
    children = Seq(
      new Label(label) { style = "-fx-text-fill: #cccccc; -fx-font-size: 14px;" },
      new Label(value) { style = "-fx-text-fill: white; -fx-font-size: 18px;" }
    )
  }
}