package poker.frontend.scenes

import poker.frontend.widgets.MainSceneWidgets.{CreateGameButton, JoinToGameButton, PokerScaleTitle}
import scalafx.geometry.{Pos, Insets}
import scalafx.scene.Scene
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.{BorderPane, VBox, StackPane, HBox}
import scalafx.scene.effect.BlendMode

object MainScene {
  def apply() : Scene = {
    new Scene :
    {
      root = new StackPane:
      {
        style = "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, #2e7d32, #1b5e20);"

        val candleLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/bag_of_money.png")) {
          fitWidth = 220
          preserveRatio = true
        }

        val candleRight = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/bag_of_money.png")) {
          fitWidth = 220
          preserveRatio = true
        }

        val cardsLeft = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/cards.png")) {
          fitWidth = 200
          preserveRatio = true
        }

        val bigWin = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/big_win.png")) {
          fitWidth = 250
          preserveRatio = true
        }

        val cardsRight = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/cards.png")) {
          fitWidth = 200
          preserveRatio = true
        }

        val middleRow = new HBox:
        {
          alignment = Pos.Center
          spacing = 20
          children = Seq(cardsLeft, bigWin, cardsRight)
        }

        val chip1 = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/chips.png")) {
          fitWidth = 130
          preserveRatio = true
        }

        val chip2 = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/chips.png")) {
          fitWidth = 130
          preserveRatio = true
        }

        val chip3 = new ImageView(new Image("file:./src/main/scala/poker/frontend/Resources/chips.png")) {
          fitWidth = 130
          preserveRatio = true
        }

        val chipsBottom = new HBox:
        {
          alignment = Pos.BottomCenter
          spacing = 15
          children = Seq(chip1, chip2, chip3)
        }

        val mainContainer = new VBox:
        {
          alignment = Pos.Center
          spacing = 35
          children = Seq(
            PokerScaleTitle.apply(),
            middleRow,
            new VBox:
              {
                alignment = Pos.Center
                spacing = 20
                children = Seq(
                  CreateGameButton.apply(),
                  JoinToGameButton.apply()
                )
              }
          )
        }

        children = Seq(candleLeft, candleRight, chipsBottom, mainContainer)

        StackPane.setAlignment(candleLeft, Pos.TopLeft)
        StackPane.setMargin(candleLeft, Insets(20))

        StackPane.setAlignment(candleRight, Pos.TopRight)
        StackPane.setMargin(candleRight, Insets(20))

        StackPane.setAlignment(chipsBottom, Pos.BottomCenter)
        StackPane.setMargin(chipsBottom, Insets(0, 0, 30, 0))
      }
    }
  }
}