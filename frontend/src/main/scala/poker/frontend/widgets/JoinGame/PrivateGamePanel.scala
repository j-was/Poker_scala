package poker.frontend.widgets.JoinGame

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, TextField}
import scalafx.scene.layout.{HBox, VBox}

object PrivateGamePanel {
  def apply(): VBox = {
    new VBox {
      alignment = Pos.Center
      spacing = 30
      padding = Insets(50)
      style =
        """
          -fx-background-color: #2a2a2a;
          -fx-background-radius: 20;
          -fx-border-color: #1a1a1a;
          -fx-border-width: 4;
          -fx-border-radius: 20;
        """

      val idRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("ID Pokoju:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 150
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz ID pokoju"
            prefWidth = 350
            style = "-fx-font-size: 20px;"
          }
        )
      }

      val passwordRow = new HBox {
        alignment = Pos.CenterRight
        spacing = 15
        maxWidth = 500
        children = Seq(
          new Label("Hasło:") {
            style = "-fx-text-fill: white; -fx-font-size: 24px;"
            prefWidth = 150
            alignment = Pos.CenterRight
          },
          new TextField {
            promptText = "Wpisz hasło"
            prefWidth = 350
            style = "-fx-font-size: 20px;"
          }
        )
      }

      children = Seq(idRow, passwordRow)
    }
  }
}