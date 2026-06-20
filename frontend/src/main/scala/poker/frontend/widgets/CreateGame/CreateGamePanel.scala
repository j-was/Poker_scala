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

      val titleLabel = new Label("Ustawienia Nowej Gry") {
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

      val modeGroup = new ToggleGroup()
      val modeToggle = GameModeToggle(modeGroup, () => {}, () => {})

      val playerNameField = new TextField {
        promptText = "Nazwa gracza"
        prefWidth = 320
        style =
          """
            -fx-font-size: 18px;
            -fx-background-color: rgba(255, 255, 255, 0.9);
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #d4af37;
            -fx-border-width: 2;
          """
      }

      val customRoomName = new TextField {
        promptText = "Podaj nazwe swojego pokoju (nie wymagane)"
        prefWidth = 320
        style =
          """
                  -fx-font-size: 18px;
                  -fx-background-color: rgba(255, 255, 255, 0.9);
                  -fx-background-radius: 10;
                  -fx-border-radius: 10;
                  -fx-border-color: #d4af37;
                  -fx-border-width: 2;
                """
      }

      def createSliderRow(labelStr: String, minVal: Int, maxVal: Int, initialVal: Int): (HBox, Slider) = {
        val valueLabel = new Label(labelStr) {
          style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"
          prefWidth = 150
        }

        val textField = new TextField {
          text = initialVal.toString
          prefWidth = 80
          style =
            """
              -fx-font-size: 16px;
              -fx-alignment: center;
              -fx-background-color: rgba(255, 255, 255, 0.9);
              -fx-background-radius: 8;
              -fx-border-radius: 8;
              -fx-border-color: #d4af37;
              -fx-border-width: 1.5;
            """
        }

        val slider = new Slider {
          min = minVal.toDouble
          max = maxVal.toDouble
          value = initialVal.toDouble
          hgrow = Priority.Always
          showTickMarks = false
          showTickLabels = false
          style =
            """
              -fx-accent: #d4af37;
              -fx-control-inner-background: rgba(0, 0, 0, 0.5);
              -fx-focus-color: transparent;
              -fx-faint-focus-color: transparent;
              -fx-padding: 10px 0;
            """
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