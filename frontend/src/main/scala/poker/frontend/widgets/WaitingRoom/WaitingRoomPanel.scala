package poker.frontend.widgets.WaitingRoom

import poker.frontend.client.PokerSession
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ListCell, ListView}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.WaitingRoom.ReadyButton
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

    new VBox {
      alignment = Pos.TopCenter
      spacing = 30
      padding = Insets(40)
      style = """
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

      val titleLabel = new Label("Poczekalnia") {
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

      val roomInfoRow = new HBox {
        alignment = Pos.Center
        spacing = 40
        style =
          """
            -fx-background-color: rgba(255, 255, 255, 0.1);
            -fx-padding: 15;
            -fx-background-radius: 10;
            -fx-border-color: #d4af37;
            -fx-border-width: 1;
            -fx-border-radius: 10;
          """
        children = Seq(
          createHeaderInfo("ID Pokoju:", code),
          createHeaderInfo("Typ:", "Prywatny")
        )
      }

      val contentRow = new HBox {
        spacing = 40
        vgrow = Priority.Always

        val playersColumn = new VBox {
          spacing = 10
          hgrow = Priority.Always

          val playersCountLabel = new Label(s"Gracze (${state.map(_.players.size).getOrElse(0)})") {
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
            style =
              """
                -fx-background-color: rgba(255, 255, 255, 0.1);
                -fx-control-inner-background: transparent;
                -fx-border-color: #d4af37;
                -fx-border-width: 2;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
              """
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

        val settingsColumn = new VBox {
          spacing = 20
          prefWidth = 300
          alignment = Pos.TopLeft
          style =
            """
              -fx-background-color: rgba(255, 255, 255, 0.1);
              -fx-padding: 25;
              -fx-background-radius: 10;
              -fx-border-color: #d4af37;
              -fx-border-width: 1;
              -fx-border-radius: 10;
            """

          children = Seq(
            new Label("USTAWIENIA") { style = "-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 20px;" },
            createSettingRow("Wpisowe:", state.map(s => s"${s.settings.initialChips}$$").getOrElse("-")),
            createSettingRow("Small Blind:", state.map(s => s"${s.settings.smallBlind}$$").getOrElse("-")),
            createSettingRow("Big Blind:", state.map(s => s"${s.settings.bigBlind}$$").getOrElse("-"))
          )
        }

        children = Seq(playersColumn, settingsColumn)
      }

      val spacer = new Region { vgrow = Priority.Always }
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