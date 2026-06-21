package poker.frontend.widgets.Game

import scalafx.scene.layout.{HBox, Priority, Region}

object Spacer {
  def apply(): Region = {
    new Region {
      HBox.setHgrow(this, Priority.Always)
    }
  }
}
