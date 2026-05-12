package poker.frontend.widgets.CreateGame

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, Slider, TextField, ToggleGroup}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.JoinGame.GameModeToggle
import poker.frontend.widgets.CreateGame.CreateButton
import poker.frontend.ScenesNavigator

object CreateGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 25
      padding = Insets(40)
      style =
        """
          -fx-background-color: #2a2a2a;
          -fx-background-radius: 20;
          -fx-border-color: #1a1a1a;
          -fx-border-width: 4;
          -fx-border-radius: 20;
        """

      val modeGroup = new ToggleGroup()
      val modeToggle = GameModeToggle(modeGroup, () => {}, () => {})

      def createSliderRow(labelStr: String, minVal: Int, maxVal: Int, initialVal: Int): (HBox, Slider) = {
        val valueLabel = new Label(labelStr) {
          style = "-fx-text-fill: white; -fx-font-size: 18px;"
          prefWidth = 150
        }

        val textField = new TextField {
          text = initialVal.toString
          prefWidth = 80
          style = "-fx-font-size: 16px; -fx-alignment: center;"
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
              -fx-accent: #4f9a3a;
              -fx-control-inner-background: #1a1a1a;
              -fx-base: red;
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
          ScenesNavigator.showWaitingRoom()
        }))
      }

      children = Seq(
        new Label("Ustawienia Nowej Gry") {
          style = "-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;"
        },
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