package poker.frontend.widgets.WaitingRoom

import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ListCell, ListView}
import scalafx.scene.layout.{HBox, Priority, Region, VBox}
import poker.frontend.widgets.WaitingRoom.ReadyButton

case class Player(nick: String, isReady: Boolean)

object WaitingRoomPanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.TopCenter
      spacing = 30
      padding = Insets(40)
      style = """
          -fx-background-color: #2a2a2a;
          -fx-background-radius: 20;
          -fx-border-color: #1a1a1a;
          -fx-border-width: 4;
          -fx-border-radius: 20;
        """

      val roomInfoRow = new HBox {
        alignment = Pos.Center
        spacing = 40
        style = "-fx-background-color: #1a1a1a; -fx-padding: 15; -fx-background-radius: 10;"
        children = Seq(
          createHeaderInfo("ID Pokoju:", "123456"),
          createHeaderInfo("Hasło:", "poker123"),
          createHeaderInfo("Typ:", "Prywatny")
        )
      }


      val contentRow = new HBox {
        spacing = 40
        vgrow = Priority.Always

        val playersColumn = new VBox {
          spacing = 10
          hgrow = Priority.Always

          val playersCountLabel = new Label("Gracze (3 / 6)") {
            style = "-fx-text-fill: #aaaaaa; -fx-font-size: 18px;"
          }

          val playersList = new ListView[Player](ObservableBuffer(
            Player("MistrzPokerowy", true),
            Player("Gacz_123", false),
            Player("Janusz_AllIn", true)
          )) {
            vgrow = Priority.Always
            style = "-fx-background-color: #1a1a1a; -fx-background-radius: 10; -fx-padding: 5;"
            cellFactory = (lv: ListView[Player]) => new ListCell[Player] {
              item.onChange { (_, _, p) =>
                if (p != null) {
                  graphic = new HBox {
                    alignment = Pos.CenterLeft
                    spacing = 15
                    children = Seq(
                      new Label(if (p.isReady) "✔" else "✘") {
                        style = s"-fx-text-fill: ${if (p.isReady) "#4f9a3a" else "#ff4444"}; -fx-font-size: 20px; -fx-font-weight: bold;"
                      },
                      new Label(p.nick) {
                        style = "-fx-text-fill: white; -fx-font-size: 18px;"
                      }
                    )
                  }
                  style = "-fx-background-color: #333333; -fx-background-radius: 5; -fx-margin: 2; -fx-padding: 10;"
                } else {
                  graphic = null
                  style = "-fx-background-color: transparent;"
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
          style = "-fx-background-color: #1a1a1a; -fx-padding: 25; -fx-background-radius: 10;"

          children = Seq(
            new Label("USTAWIENIA") { style = "-fx-text-fill: #4f9a3a; -fx-font-weight: bold; -fx-font-size: 20px;" },
            createSettingRow("Wpisowe:", "1000 $"),
            createSettingRow("Small Blind:", "10 $"),
            createSettingRow("Big Blind:", "20 $")
          )
        }

        children = Seq(playersColumn, settingsColumn)
      }

      val spacer = new Region { vgrow = Priority.Always }
      val footer = new HBox {
        alignment = Pos.BottomRight
        children = Seq(ReadyButton(() => println("Zgłoszono gotowość!")))
      }

      children = Seq(roomInfoRow, contentRow, spacer, footer)
    }
  }

  private def createHeaderInfo(label: String, value: String): VBox = new VBox {
    alignment = Pos.Center
    children = Seq(
      new Label(label) { style = "-fx-text-fill: #888888; -fx-font-size: 14px;" },
      new Label(value) { style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;" }
    )
  }

  private def createSettingRow(label: String, value: String): VBox = new VBox {
    spacing = 5
    children = Seq(
      new Label(label) { style = "-fx-text-fill: #888888; -fx-font-size: 14px;" },
      new Label(value) { style = "-fx-text-fill: white; -fx-font-size: 18px;" }
    )
  }
}