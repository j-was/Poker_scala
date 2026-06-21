package poker.frontend.client

import poker.protocol.{ClientMessage, ServerMessage}

import scala.concurrent.Future

trait PokerClientApi {
  def send(msg: ClientMessage): Boolean
  def connect(): Future[Unit]
  def onMessage(handler: ServerMessage => Unit): Unit
}