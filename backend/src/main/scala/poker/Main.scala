package poker

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import poker.actor.{AutoFoldService, GameRegistry, SessionRegistry}
import poker.server.WebSocketServer

import scala.concurrent.ExecutionContext

/**
 * Application entry point.
 *
 * Configuration via environment variables:
 *   POKER_HOST  – bind address (default: 0.0.0.0)
 *   POKER_PORT  – bind port    (default: 8080)
 */
object Main {
  def main(args: Array[String]): Unit = {

  val host = sys.env.getOrElse("POKER_HOST", "0.0.0.0");
  val port = sys.env.get("POKER_PORT").flatMap(_.toIntOption).getOrElse(8080);

  val guardian = Behaviors.setup[Nothing] { ctx =>
    val sessionRegistry = ctx.spawn(SessionRegistry(), "session-registry")
    val autoFoldService = ctx.spawn(AutoFoldService(), "auto-fold")
    val gameRegistry = ctx.spawn(
      GameRegistry(Some(autoFoldService), Some(sessionRegistry)),
      "game-registry"
    )

    ctx.log.info(s"All registries started. Starting WebSocket server on $host:$port")

    given system: ActorSystem[?] = ctx.system

    WebSocketServer.start(host, port, sessionRegistry, gameRegistry)

    Behaviors.empty
  }

  val system = ActorSystem[Nothing](guardian, "poker-server")

  sys.addShutdownHook {
    system.log.info("Shutting down poker server…")
    system.terminate()
  }

    Thread.currentThread().join();
  }
}