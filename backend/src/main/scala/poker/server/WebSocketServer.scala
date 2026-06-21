package poker.server

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.ws.{Message, TextMessage}
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.stream.typed.scaladsl.ActorSource
import io.circe.syntax.*
import io.circe.parser.*
import poker.actor.{ClientConnection, GameRegistry, SessionRegistry}
import poker.protocol.{ClientMessage, JsonCodecs, ServerMessage}
import poker.protocol.JsonCodecs.given

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object WebSocketServer {
  /**
   * Builds the WebSocket flow for one client:
   */
  private def buildFlow(
                         sessionRegistry: ActorRef[SessionRegistry.Command],
                         gameRegistry: ActorRef[GameRegistry.Command]
                       )(using system: ActorSystem[?]): Flow[Message, Message, ?] = {

    val (outgoingActor, outgoingSource) =
      ActorSource
        .actorRef[ServerMessage](
          completionMatcher = PartialFunction.empty,
          failureMatcher = PartialFunction.empty,
          bufferSize = 256,
          overflowStrategy = OverflowStrategy.dropHead
        )
        .preMaterialize()


    val connectionActor: ActorRef[ClientConnection.Command] = {
      system.systemActorOf(
        ClientConnection(sessionRegistry, gameRegistry, outgoingActor),
        s"connection-${java.util.UUID.randomUUID()}"
      )
    }

    val incomingSink: Sink[Message, ?] = Sink.foreach[Message] {
      case TextMessage.Strict(text) => {
        decode[ClientMessage](text) match
          case Right(msg) => connectionActor ! ClientConnection.IncomingMessage(msg)
          case Left(err) =>
            outgoingActor ! ServerMessage.Error(s"Invalid message: ${err.getMessage}")
      }
      case _: TextMessage.Streamed =>
        ()
      case _ =>
        ()
    }

    val outgoingMapped = outgoingSource.map(msg => TextMessage(msg.asJson.noSpaces))

    Flow.fromSinkAndSource(incomingSink, outgoingMapped)
      .watchTermination() { (_, done) =>
        implicit val ec: ExecutionContext = system.executionContext
        done.onComplete(_ => connectionActor ! ClientConnection.ConnectionClosed)
      }
  }

  def start(
             host: String,
             port: Int,
             sessionRegistry: ActorRef[SessionRegistry.Command],
             gameRegistry: ActorRef[GameRegistry.Command]
           )(using system: ActorSystem[?]): Future[Http.ServerBinding] = {

    given ExecutionContext = system.executionContext

    val route = {
      path("ws") {
        handleWebSocketMessages(buildFlow(sessionRegistry, gameRegistry))
      } ~
        path("health") {
          get {
            complete("OK")
          }
        }
    }

    Http()
      .newServerAt(host, port)
      .bind(route)
      .andThen {
        case Success(binding) =>
          system.log.info(s"Poker server started at ws://$host:$port/ws")
        case Failure(ex) => {
          system.log.error(s"Failed to start server: ${ex.getMessage}")
          system.terminate()
        }
      }

  }
}