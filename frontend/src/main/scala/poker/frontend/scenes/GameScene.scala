package poker.frontend.scenes

import poker.domain.{Board, Card, ClientGameState, ClientPlayer, GameStatus, Suit}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label, TextField}
import scalafx.scene.layout.{BorderPane, FlowPane, HBox, Priority, Region, StackPane, VBox}
import scalafx.scene.paint.Color

object GameScene {
  def apply(
     code: String,
     myPlayerId: String,
     state: ClientGameState,
     onFold: () => Unit = () => (),
     onCall: () => Unit = () => (),
     onCheck: () => Unit = () => (),
     onRaise: Int => Unit = _ => (),
     onStartGame: () => Unit = () => (),
     onLeave: () => Unit = () => ()
  ) : Scene = {
    new Scene {
      root = new BorderPane {
        style = "-fx-background-color: #235b32;"
        padding = Insets(20)

        top = infoBar(code, state, onLeave)
        center = tableArea(myPlayerId, state)
        bottom = bottomPanel(myPlayerId, state, onFold, onCall, onCheck, onRaise, onStartGame)
      }
    }
  }

  private def infoBar(code: String, state: ClientGameState, onLeave: () => Unit): HBox = {
    new HBox {
      alignment = Pos.CenterLeft
      spacing = 24
      padding = Insets(0, 0, 20, 0)

      children = Seq(
        label(s"Pokój: $code", 20, bold = true),
        label(s"Status: ${state.status}", 18),
        label(s"Pula: ${state.pot}", 18),
        label(s"Najwyższy zakład: ${state.currentHighestBet}", 18),
        spacer(),
        new Button("Opuść") {
          style = buttonStyle("#8b2f2f")
          onAction = _ => onLeave()
        }
      )
    }
  }

  private def tableArea(myPlayerId: String, state: ClientGameState): BorderPane = {
    val currPlayerId = state.players.lift(state.currentPlayerIndex).map(_.id)

    new BorderPane {
      center = new VBox {
        alignment = Pos.Center
        spacing = 24

        children = Seq(
          playersRow(myPlayerId, state, currPlayerId),
          boardView(state.board),
          potView(state)
        )
      }
    }
  }

  private def playersRow(myPlayerId: String, state: ClientGameState, currPlayerId: Option[String]) : FlowPane = {
    new FlowPane {
      alignment = Pos.Center
      hgap = 16
      vgap = 16

      children = state.players.zipWithIndex.map {case (player, index) =>
        playerSeat(player = player, isMe = player.id == myPlayerId, isDealer = index == state.dealerIndex,
          isCurrent = currPlayerId.contains(player.id))
      }
    }
  }

  private def playerSeat(player: ClientPlayer, isMe: Boolean, isDealer: Boolean, isCurrent: Boolean) : VBox = {
    val borderColor = if isCurrent then "#ffd54f" else if isMe then "#64b5f6" else "#1f1f1f"

    new VBox {
      alignment = Pos.Center
      spacing = 8
      padding = Insets(14)
      prefWidth = 190

      style = s"""
           -fx-background-color: #2b2b2b;
           -fx-background-radius: 10;
           -fx-border-color: $borderColor;
           -fx-border-width: 3;
           -fx-border-radius: 10;
        """

      children = Seq(
        label(player.name + (if isMe then " (TY)" else "") + (if isDealer then " D" else ""), 18, bold = true),
        label(s"Żetony: ${player.chips}", 15),
        label(s"Zakład: ${player.currentBet}", 15),
        label(if player.isActive then "Aktywny" else "Pass", 15),
        hiddenCards(player.hasCards)
      )
    }
  }

  private def boardView(board: Board): HBox = {
    new HBox {
      alignment = Pos.Center
      spacing = 12

      val cards = board.cards

      children = if cards.isEmpty then Seq(label("Pre-Flop", 26, bold = true)) else cards.map(cardView)
    }
  }

  private def potView(state: ClientGameState): VBox = {
    new VBox {
      alignment = Pos.Center
      spacing = 6

      children = Seq(
        label(s"Pula: ${state.pot}", 24, bold = true),
        label(s"Small blind: ${state.settings.smallBlind}  Big blind: ${state.settings.bigBlind}", 16)
      )
    }
  }

  private def bottomPanel(myPlayerId: String, state: ClientGameState,
                           onFold: () => Unit,
                           onCall: () => Unit,
                           onCheck: () => Unit,
                           onRaise: Int => Unit,
                           onStartGame: () => Unit
                         ): VBox = {
    new VBox {
      spacing = 16
      padding = Insets(20, 0, 0, 0)
      alignment = Pos.Center

      children = Seq(
        myHandPanel(state),
        actionPanel(myPlayerId, state, onFold, onCall, onCheck, onRaise, onStartGame)
      )
    }
  }

  private def myHandPanel(state: ClientGameState): VBox = {
    val cards = state.myHoleCards.map(_.toList).getOrElse(Nil)

    new VBox {
      alignment = Pos.Center
      spacing = 10

      children = Seq(
        new HBox {
          alignment = Pos.Center
          spacing = 12
          children =
            if cards.isEmpty then Seq(label("Brak kart", 20))
            else cards.map(cardView)
        },
        label(state.myHandCategory.map(category => s"Układ: $category").getOrElse("Układ: -"), 18, bold = true)
      )
    }
  }

  private def actionPanel(
                           myPlayerId: String,
                           state: ClientGameState,
                           onFold: () => Unit,
                           onCall: () => Unit,
                           onCheck: () => Unit,
                           onRaise: Int => Unit,
                           onStartGame: () => Unit
                         ): HBox = {
    val isMyTurn =
      state.status == GameStatus.Playing &&
        state.players.lift(state.currentPlayerIndex).exists(_.id == myPlayerId)

    val raiseInput = new TextField {
      promptText = "Raise"
      prefWidth = 100
      disable = !isMyTurn
      style = "-fx-font-size: 16px;"
    }

    new HBox {
      alignment = Pos.Center
      spacing = 12

      children = Seq(
        new Button("Fold") {
          disable = !isMyTurn
          style = buttonStyle("#7a2d2d")
          onAction = _ => onFold()
        },
        new Button("Check") {
          disable = !isMyTurn
          style = buttonStyle("#2f5f8f")
          onAction = _ => onCheck()
        },
        new Button("Call") {
          disable = !isMyTurn
          style = buttonStyle("#2f7a4f")
          onAction = _ => onCall()
        },
        raiseInput,
        new Button("Raise") {
          disable = !isMyTurn
          style = buttonStyle("#8a6a20")
          onAction = _ => {
            raiseInput.text.value.toIntOption.foreach(onRaise)
          }
        },
        new Button("Start") {
          disable = state.status == GameStatus.Playing
          style = buttonStyle("#4f7f3f")
          onAction = _ => onStartGame()
        }
      )
    }
  }

  private def cardView(card: Card): StackPane = {
    val red =
      card.suit == Suit.Hearts || card.suit == Suit.Diamonds

    new StackPane {
      prefWidth = 72
      prefHeight = 104
      maxWidth = 72
      maxHeight = 104

      style =
        """
              -fx-background-color: white;
              -fx-background-radius: 8;
              -fx-border-color: #111111;
              -fx-border-width: 2;
              -fx-border-radius: 8;
            """

      children = Seq(
        new Label(card.toString) {
          textFill = if red then Color.DarkRed else Color.Black
          style = "-fx-font-size: 26px; -fx-font-weight: bold;"
        }
      )
    }
  }

  private def hiddenCards(hasCards: Boolean): HBox = {
    new HBox {
      alignment = Pos.Center
      spacing = 6

      children =
        if !hasCards then Seq(label("-", 16))
        else Seq(cardBack(), cardBack())
    }
  }

  private def cardBack(): StackPane = {
    new StackPane {
      prefWidth = 34
      prefHeight = 48
      maxWidth = 34
      maxHeight = 48

      style =
        """
              -fx-background-color: #1d3f73;
              -fx-background-radius: 5;
              -fx-border-color: white;
              -fx-border-width: 1;
              -fx-border-radius: 5;
            """
    }
  }

  private def label(textValue: String, size: Int, bold: Boolean = false): Label = {
    new Label(textValue) {
      textFill = Color.White
      style =
        s"""
               -fx-font-size: ${size}px;
               ${if bold then "-fx-font-weight: bold;" else ""}
            """
    }
  }

  private def spacer(): Region = {
    new Region {
      HBox.setHgrow(this, Priority.Always)
    }
  }

  private def buttonStyle(color: String): String =
    s"""
           -fx-background-color: $color;
           -fx-text-fill: white;
           -fx-font-size: 16px;
           -fx-font-weight: bold;
           -fx-background-radius: 8;
           -fx-cursor: hand;
           -fx-padding: 8 16 8 16;
        """
}
