package poker.frontend.client

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.ws.*
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.pekko.stream.{KillSwitches, OverflowStrategy}
import io.circe.syntax.*
import io.circe.parser.*
import poker.protocol.{ClientMessage, ServerMessage, JsonCodecs}
import poker.protocol.JsonCodecs.given

import java.util.concurrent.LinkedBlockingQueue
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
 * Manages the WebSocket connection to the poker server from the desktop client.
 *
 * Usage (ScalaFX side):
 * {{{
 *   val client = PokerClient("ws://localhost:8080/ws")
 *   client.onMessage { msg => Platform.runLater { updateUI(msg) } }
 *   client.connect()
 *   client.send(ClientMessage.Identify("Alice", storedPlayerId))
 * }}}
 */
val isDebug = false
class PokerClient(serverUrl: String) extends PokerClientApi {

  private given system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "poker-client")

  private given ExecutionContext = system.executionContext

  @volatile private var messageHandler: ServerMessage => Unit = _ => ()
  @volatile private var connectionHandler: Boolean => Unit = _ => ()

  private val sendQueue = new LinkedBlockingQueue[ClientMessage](512)

  def onMessage(handler: ServerMessage => Unit): Unit =
    messageHandler = handler

  def onConnectionChange(handler: Boolean => Unit): Unit =
    connectionHandler = handler

  /**
   * Enqueue a message to be sent to the server.
   * Returns false if the queue is full (backpressure; should not happen in normal play).
   */
  def send(msg: ClientMessage): Boolean = {
    if(isDebug) {
      println(s"[WYCHODZĄCE] Obiekt: $msg")
      println(s"[WYCHODZĄCE] JSON: ${msg.asJson.noSpaces}")
    }
    sendQueue.offer(msg)
  }

  /**
   * Establish (or re-establish) the WebSocket connection.
   * Safe to call multiple times; each call replaces any previous connection.
   */
  def connect(): Future[Unit] = {
    val connected = Promise[Unit]()
    if (isDebug) {
      println(s"${serverUrl}")
    }
    val outgoingSource: Source[Message, ?] = {
      Source
        .queue[Message](512, OverflowStrategy.dropHead)
        .mapMaterializedValue { queue => {
          val drainer = new Thread(() => {
            while !Thread.currentThread().isInterrupted do
              try
                val msg = sendQueue.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                if msg != null then
                  queue.offer(TextMessage(msg.asJson.noSpaces))
              catch
                case _: InterruptedException => Thread.currentThread().interrupt()
          }, "ws-send-drainer")

          drainer.setDaemon(true)
          drainer.start()
          queue
        }
        }
    }


    val incomingSink: Sink[Message, ?] = {
      Sink.foreach[Message] {
        case TextMessage.Strict(text) =>
          if(isDebug) {
            println(s"[PRZYCHODZĄCE] JSON: $text")
          }
          decode[ServerMessage](text) match
            case Right(msg) =>
              if(isDebug) {
                println(s"[ZDEKODOWANE] $msg")
              }
              messageHandler(msg)
            case Left(err) =>
              println(s"[BŁĄD DEKODOWANIA] Nie udało się zdekodować wiadomości serwera: $err")
              println(s"[BŁĄD DEKODOWANIA] Surowy JSON: $text")
              system.log.warn(s"Could not decode server message: $err")
        case _ => ()
      }
    }


    val flow: Flow[Message, Message, ?] =
      Flow.fromSinkAndSourceMat(incomingSink, outgoingSource)(Keep.right)

    val (upgradeResponse, _) =
      Http()
        .singleWebSocketRequest(WebSocketRequest(serverUrl), flow)

    upgradeResponse
      .flatMap { upgrade => {
        if upgrade.response.status.isSuccess() then {
          connected.success(())
          connectionHandler(true)
          Future.successful(())
        }
        else {
          if (isDebug) {
            println(s"[BŁĄD POŁĄCZENIA] Odrzucono: ${upgrade.response.status}")
          }
          val ex = new RuntimeException(s"WebSocket upgrade failed: ${upgrade.response.status}")
          connected.failure(ex)
          Future.failed(ex)
        }
      }
      }
      .recover { case ex => {
        if (isDebug) {
          println(s"[BŁĄD POŁĄCZENIA] Awaria sieci: ${ex.getMessage}")
        }
        connectionHandler(false)
        system.log.error(s"WebSocket connection failed: ${ex.getMessage}")
      }
      }

    connected.future
  }

  /**
   * Attempt to reconnect with exponential back-off.
   * Intended to be called from the onConnectionChange(false) callback.
   */
  def reconnectWithBackoff(
                            maxAttempts: Int = 10,
                            initialDelay: FiniteDuration = 1.second
                          ): Unit = {
    def attempt(n: Int, delay: FiniteDuration): Unit = {
      if n > maxAttempts then
        system.log.error("Max reconnect attempts reached; giving up")
      else {
        system.log.info(s"Reconnect attempt $n in $delay…")
        system.scheduler.scheduleOnce(delay, () => {
          connect().onComplete {
            case Success(_) => () // connected
            case Failure(ex) =>
              system.log.warn(s"Reconnect $n failed: ${ex.getMessage}")
              attempt(n + 1, (delay * 2).min(30.seconds))
          }
        })(system.executionContext)
      }
    }

    attempt(1, initialDelay)
  }

  /** Cleanly shut down the client and its actor system. */
  def shutdown(): Unit =
    system.terminate()
}