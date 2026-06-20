package poker.frontend.widgets.Shared

import scalafx.scene.control.Alert
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ButtonType
import scalafx.stage.StageStyle
import scalafx.Includes.jfxNode2sfx

object ErrorDialog {
  def show(message: String): Unit = {
    val alert = new Alert(AlertType.Error) {
      initStyle(StageStyle.Undecorated)
      headerText = null: String
      contentText = message
    }

    val dialogPane = alert.dialogPane()

    dialogPane.style =
      """
        -fx-background-color: #2a2a2a;
        -fx-border-color: #ef4444;
        -fx-border-width: 7;
      """

    val contentLabel = dialogPane.lookup(".content.label")
    if (contentLabel != null) {
      contentLabel.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 20px;"
    }

    val okButton = dialogPane.lookupButton(ButtonType.OK)
    if (okButton != null) {
      okButton.style =
        """
          -fx-background-color: #ef4444;
          -fx-text-fill: white;
          -fx-font-size: 16px;
          -fx-font-weight: bold;
          -fx-background-radius: 8;
          -fx-cursor: hand;
          -fx-padding: 8px 20px;
        """
    }

    alert.showAndWait()
  }
}