package poker.protocol

import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import poker.domain.*

// ─── Client → Server ─────────────────────────────────────────────────────────
sealed trait ClientMessage

object ClientMessage:
  case class Identify(name: String, playerId: Option[String]) extends ClientMessage
  case class CreateGame(settings: GameSettings)               extends ClientMessage
  case class JoinGame(code: String)                           extends ClientMessage
  case class StartGame(code: String)                          extends ClientMessage
  case class Fold(code: String)                               extends ClientMessage
  case class Call(code: String)                               extends ClientMessage
  case class Check(code: String)                              extends ClientMessage
  case class Raise(code: String, amount: Int)                 extends ClientMessage
  case class LeaveGame(code: String)                          extends ClientMessage
  case object Ping                                            extends ClientMessage

// ─── Server → Client ─────────────────────────────────────────────────────────
sealed trait ServerMessage
object ServerMessage:
  case class Identified(playerId: String, name: String)              extends ServerMessage
  case class GameCreated(code: String, state: ClientGameState)       extends ServerMessage
  case class GameJoined(code: String, state: ClientGameState)        extends ServerMessage
  case class GameStarted(code: String, state: ClientGameState)       extends ServerMessage
  case class StateUpdate(code: String, state: ClientGameState)       extends ServerMessage
  case class GameOver(code: String, winnerId: String, winnerName: String, state: ClientGameState) extends ServerMessage
  case class PlayerJoined(code: String, playerId: String, name: String) extends ServerMessage
  case class PlayerLeft(code: String, playerId: String, name: String)   extends ServerMessage
  case class PlayerDisconnected(code: String, playerId: String, name: String) extends ServerMessage
  case class PlayerReconnected(code: String, playerId: String, name: String) extends ServerMessage
  case class Error(message: String)                                  extends ServerMessage
  case object Pong                                                   extends ServerMessage

// ─── JSON codecs ─────────────────────────────────────────────────────────────
object JsonCodecs:
  given Encoder[Suit]    = Encoder.encodeString.contramap(_.toString)
  given Decoder[Suit]    = Decoder.decodeString.emap(s => Suit.values.find(_.toString == s).toRight(s"Unknown suit: $s"))
  given Encoder[Rank]    = Encoder.encodeString.contramap(_.toString)
  given Decoder[Rank]    = Decoder.decodeString.emap(s => Rank.values.find(_.toString == s).toRight(s"Unknown rank: $s"))
  given Encoder[Card]    = deriveEncoder
  given Decoder[Card]    = deriveDecoder
  given Encoder[HoleCards] = deriveEncoder
  given Decoder[HoleCards] = deriveDecoder
  given Encoder[HandCategory] = Encoder.encodeString.contramap(_.toString)
  given Decoder[HandCategory] = Decoder.decodeString.emap(s => HandCategory.values.find(_.toString == s).toRight(s"Unknown category: $s"))
  given Encoder[GameStatus]   = Encoder.encodeString.contramap(_.toString)
  given Decoder[GameStatus]   = Decoder.decodeString.emap(s => GameStatus.values.find(_.toString == s).toRight(s"Unknown status: $s"))
  given Encoder[GameSettings] = deriveEncoder
  given Decoder[GameSettings] = deriveDecoder

  given Encoder[Board] = Encoder.instance {
    case Board.PreFlop              => Json.obj("street" -> "PreFlop".asJson)
    case Board.Flop(c1, c2, c3)    => Json.obj("street" -> "Flop".asJson,  "cards" -> List(c1, c2, c3).asJson)
    case Board.Turn(c1, c2, c3, c4)=> Json.obj("street" -> "Turn".asJson,  "cards" -> List(c1, c2, c3, c4).asJson)
    case Board.River(c1,c2,c3,c4,c5)=> Json.obj("street"-> "River".asJson, "cards" -> List(c1,c2,c3,c4,c5).asJson)
  }
  given Decoder[Board] = Decoder.instance { cursor =>
    for
      street <- cursor.downField("street").as[String]
      board  <- street match
        case "PreFlop" => Right(Board.PreFlop)
        case "Flop"    => cursor.downField("cards").as[List[Card]].map(cs => Board.Flop(cs(0), cs(1), cs(2)))
        case "Turn"    => cursor.downField("cards").as[List[Card]].map(cs => Board.Turn(cs(0), cs(1), cs(2), cs(3)))
        case "River"   => cursor.downField("cards").as[List[Card]].map(cs => Board.River(cs(0), cs(1), cs(2), cs(3), cs(4)))
        case other     => Left(DecodingFailure(s"Unknown street: $other", cursor.history))
    yield board
  }

  given Encoder[ClientPlayer]    = deriveEncoder
  given Decoder[ClientPlayer]    = deriveDecoder
  given Encoder[ClientGameState] = deriveEncoder
  given Decoder[ClientGameState] = deriveDecoder

  // ── ClientMessage ──
  given Encoder[ClientMessage] = Encoder.instance {
    case m: ClientMessage.Identify  => Json.obj("type" -> "Identify".asJson,  "name" -> m.name.asJson, "playerId" -> m.playerId.asJson)
    case m: ClientMessage.CreateGame=> Json.obj("type" -> "CreateGame".asJson, "settings" -> m.settings.asJson)
    case m: ClientMessage.JoinGame  => Json.obj("type" -> "JoinGame".asJson,   "code" -> m.code.asJson)
    case m: ClientMessage.StartGame => Json.obj("type" -> "StartGame".asJson,  "code" -> m.code.asJson)
    case m: ClientMessage.Fold      => Json.obj("type" -> "Fold".asJson,       "code" -> m.code.asJson)
    case m: ClientMessage.Call      => Json.obj("type" -> "Call".asJson,       "code" -> m.code.asJson)
    case m: ClientMessage.Check     => Json.obj("type" -> "Check".asJson,      "code" -> m.code.asJson)
    case m: ClientMessage.Raise     => Json.obj("type" -> "Raise".asJson,      "code" -> m.code.asJson, "amount" -> m.amount.asJson)
    case m: ClientMessage.LeaveGame => Json.obj("type" -> "LeaveGame".asJson,  "code" -> m.code.asJson)
    case ClientMessage.Ping         => Json.obj("type" -> "Ping".asJson)
  }

  given Decoder[ClientMessage] = Decoder.instance { cursor =>
    cursor.downField("type").as[String].flatMap {
      case "Identify"  => for name <- cursor.downField("name").as[String]; pid <- cursor.downField("playerId").as[Option[String]] yield ClientMessage.Identify(name, pid)
      case "CreateGame"=> cursor.downField("settings").as[GameSettings].map(ClientMessage.CreateGame.apply)
      case "JoinGame"  => cursor.downField("code").as[String].map(ClientMessage.JoinGame.apply)
      case "StartGame" => cursor.downField("code").as[String].map(ClientMessage.StartGame.apply)
      case "Fold"      => cursor.downField("code").as[String].map(ClientMessage.Fold.apply)
      case "Call"      => cursor.downField("code").as[String].map(ClientMessage.Call.apply)
      case "Check"     => cursor.downField("code").as[String].map(ClientMessage.Check.apply)
      case "Raise"     => for code <- cursor.downField("code").as[String]; amt <- cursor.downField("amount").as[Int] yield ClientMessage.Raise(code, amt)
      case "LeaveGame" => cursor.downField("code").as[String].map(ClientMessage.LeaveGame.apply)
      case "Ping"      => Right(ClientMessage.Ping)
      case other       => Left(DecodingFailure(s"Unknown client message type: $other", cursor.history))
    }
  }

  // ── ServerMessage ──
  given Encoder[ServerMessage] = Encoder.instance {
    case m: ServerMessage.Identified       => Json.obj("type" -> "Identified".asJson,       "playerId" -> m.playerId.asJson, "name" -> m.name.asJson)
    case m: ServerMessage.GameCreated      => Json.obj("type" -> "GameCreated".asJson,      "code" -> m.code.asJson, "state" -> m.state.asJson)
    case m: ServerMessage.GameJoined       => Json.obj("type" -> "GameJoined".asJson,       "code" -> m.code.asJson, "state" -> m.state.asJson)
    case m: ServerMessage.GameStarted      => Json.obj("type" -> "GameStarted".asJson,      "code" -> m.code.asJson, "state" -> m.state.asJson)
    case m: ServerMessage.StateUpdate      => Json.obj("type" -> "StateUpdate".asJson,      "code" -> m.code.asJson, "state" -> m.state.asJson)
    case m: ServerMessage.GameOver         => Json.obj("type" -> "GameOver".asJson,         "code" -> m.code.asJson, "winnerId" -> m.winnerId.asJson, "winnerName" -> m.winnerName.asJson, "state" -> m.state.asJson)
    case m: ServerMessage.PlayerJoined     => Json.obj("type" -> "PlayerJoined".asJson,     "code" -> m.code.asJson, "playerId" -> m.playerId.asJson, "name" -> m.name.asJson)
    case m: ServerMessage.PlayerLeft       => Json.obj("type" -> "PlayerLeft".asJson,       "code" -> m.code.asJson, "playerId" -> m.playerId.asJson, "name" -> m.name.asJson)
    case m: ServerMessage.PlayerDisconnected=>Json.obj("type" -> "PlayerDisconnected".asJson,"code" -> m.code.asJson, "playerId" -> m.playerId.asJson, "name" -> m.name.asJson)
    case m: ServerMessage.PlayerReconnected=> Json.obj("type" -> "PlayerReconnected".asJson, "code" -> m.code.asJson, "playerId" -> m.playerId.asJson, "name" -> m.name.asJson)
    case m: ServerMessage.Error            => Json.obj("type" -> "Error".asJson,            "message" -> m.message.asJson)
    case ServerMessage.Pong                => Json.obj("type" -> "Pong".asJson)
  }

  given Decoder[ServerMessage] = Decoder.instance { cursor =>
    cursor.downField("type").as[String].flatMap {
      case "Identified"        => for pid <- cursor.downField("playerId").as[String]; n <- cursor.downField("name").as[String] yield ServerMessage.Identified(pid, n)
      case "GameCreated"       => for code <- cursor.downField("code").as[String]; st <- cursor.downField("state").as[ClientGameState] yield ServerMessage.GameCreated(code, st)
      case "GameJoined"        => for code <- cursor.downField("code").as[String]; st <- cursor.downField("state").as[ClientGameState] yield ServerMessage.GameJoined(code, st)
      case "GameStarted"       => for code <- cursor.downField("code").as[String]; st <- cursor.downField("state").as[ClientGameState] yield ServerMessage.GameStarted(code, st)
      case "StateUpdate"       => for code <- cursor.downField("code").as[String]; st <- cursor.downField("state").as[ClientGameState] yield ServerMessage.StateUpdate(code, st)
      case "GameOver"          => for code <- cursor.downField("code").as[String]; wid <- cursor.downField("winnerId").as[String]; wn <- cursor.downField("winnerName").as[String]; st <- cursor.downField("state").as[ClientGameState] yield ServerMessage.GameOver(code, wid, wn, st)
      case "PlayerJoined"      => for code <- cursor.downField("code").as[String]; pid <- cursor.downField("playerId").as[String]; n <- cursor.downField("name").as[String] yield ServerMessage.PlayerJoined(code, pid, n)
      case "PlayerLeft"        => for code <- cursor.downField("code").as[String]; pid <- cursor.downField("playerId").as[String]; n <- cursor.downField("name").as[String] yield ServerMessage.PlayerLeft(code, pid, n)
      case "PlayerDisconnected"=> for code <- cursor.downField("code").as[String]; pid <- cursor.downField("playerId").as[String]; n <- cursor.downField("name").as[String] yield ServerMessage.PlayerDisconnected(code, pid, n)
      case "PlayerReconnected" => for code <- cursor.downField("code").as[String]; pid <- cursor.downField("playerId").as[String]; n <- cursor.downField("name").as[String] yield ServerMessage.PlayerReconnected(code, pid, n)
      case "Error"             => cursor.downField("message").as[String].map(ServerMessage.Error.apply)
      case "Pong"              => Right(ServerMessage.Pong)
      case other               => Left(DecodingFailure(s"Unknown server message type: $other", cursor.history))
    }
  }