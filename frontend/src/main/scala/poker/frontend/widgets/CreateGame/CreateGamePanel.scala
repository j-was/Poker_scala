package poker.frontend.widgets.CreateGame

import poker.domain.GameSettings
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, Slider, TextField, ToggleGroup}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.JoinGame.GameModeToggle
import poker.frontend.ScenesNavigator
import poker.frontend.client.PokerSession
import poker.frontend.widgets.Shared.ErrorDialog
import scalafx.scene.effect.DropShadow
import scalafx.scene.paint.Color

object CreateGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 25
      padding = Insets(40)
      styleClass += "create-game-panel"

      val dollarsLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/dolar.png")) {
        fitWidth = 60
        preserveRatio = true
      }

      val titleLabel = new Label("Ustawienia Nowej Gry") {
        styleClass += "create-game-panel-title"
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

      val modeGroup = new ToggleGroup()
      val modeToggle = GameModeToggle(modeGroup, () => {}, () => {})

      val playerNameField = new TextField {
        promptText = "Nazwa gracza"
        prefWidth = 320
        styleClass += "create-game-panel-player-name"
      }

      val customRoomName = new TextField {
        promptText = "Podaj nazwę swojego pokoju (nie wymagane)"
        prefWidth = 320
        styleClass += "create-game-panel-custom-room-name"
      }

      def createSliderRow(labelStr: String, minVal: Int, maxVal: Int, initialVal: Int): (HBox, Slider) = {
        val valueLabel = new Label(labelStr) {
          styleClass += "create-game-panel-slider-label"
          prefWidth = 150
        }

        val textField = new TextField {
          text = initialVal.toString
          prefWidth = 80
          styleClass += "create-game-panel-slider-text"
        }

        val slider = new Slider {
          min = minVal.toDouble
          max = maxVal.toDouble
          value = initialVal.toDouble
          hgrow = Priority.Always
          showTickMarks = false
          showTickLabels = false
          styleClass += "create-game-panel-slider"
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
          spacing = 20
          children = Seq(valueLabel, slider, textField)
        }

        (row, slider)
      }

      val (maxPlayersRow, _) = createSliderRow("Max graczy:", 4, 10, 6)
      val (buyInRow, buyInSlider) = createSliderRow("Wpisowe ($):", 100, 10000, 1000)
      val (smallBlindRow, smallBlindSlider) = createSliderRow("Small Blind:", 5, 500, 10)
      val (bigBlindRow, bigBlindSlider) = createSliderRow("Big Blind:", 10, 1000, 20)

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

      val spacer = new Region { vgrow = Priority.Always }

      val bottomRow = new HBox {
        alignment = Pos.BottomRight
        children = Seq(CreateButton(() =>
        {
          PokerSession.configure(
            name = playerNameField.text.value,
            stateHandler = ScenesNavigator.showServerState,
            errorHandler = msg => ErrorDialog.show(msg)
          )

          PokerSession.createGame(GameSettings(
            smallBlind = smallBlindSlider.value.value.toInt,
            bigBlind = bigBlindSlider.value.value.toInt,
            initialChips = buyInSlider.value.value.toInt
          ))
        }))
      }

      children = Seq(
        titleRow,
        playerNameField,
        customRoomName,
        modeToggle,
        maxPlayersRow,
        buyInRow,
        smallBlindRow,
        bigBlindRow,
        spacer,
        bottomRow
      )
    }
  }
}