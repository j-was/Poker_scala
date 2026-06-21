# Engine – How to use it

The engine has two layers:

- **`GameInstance`** – Pekko actor. Handles the game state and incoming events.
- **`PokerEngine`** – pure game logic (dealing, phases, pot). Called automatically by `GameInstance`.

---

## Commands

```scala
import poker.actor.GameInstance
import poker.actor.GameInstance.*
import poker.domain.GameSettings

// Spawn a table
val table = system.spawn(
  GameInstance("table-1", GameSettings(
    name = "Friday Night",
    smallBlind = 10,
    bigBlind = 20,
    initialChips = 1000,
    isPublic = true,
    maxPlayers = 6
  )),
  "table-1"
)

// Available commands (all require a replyTo: ActorRef[Response])
table ! JoinGame("player-id", "Alice", replyTo)
table ! StartGame(replyTo)
table ! Fold("player-id", replyTo)
table ! Call("player-id", replyTo)
table ! Check("player-id", replyTo)
table ! Raise("player-id", amount = 50, replyTo)
table ! LeaveGame("player-id", replyTo)
table ! UpdateSettings(GameSettings(name = "New Name", smallBlind = 25), replyTo)
table ! GetState(replyTo)  // returns raw GameState
```

---

## Responses

```scala
case GameJoined(playerId, state)        // player joined successfully
case GameStarted(state)                 // hand started, cards dealt, blinds posted
case ActionSuccess(state)               // action accepted, hand ended, next hand pending
case GameOver(winnerId, state)          // tournament finished – winnerId has all the chips
case SettingsUpdated(state)             // settings changed, new state returned
case Error(msg: String)                 // rejected – see error table below
```

> `state` is always a raw `GameState`. Never send it to a client directly – see below.

---

## Sending state to players

Each player must receive **their own view** of the state – other players' hole cards must be hidden.

```scala
import poker.domain.*  // brings toClientView into scope

def broadcastState(state: GameState): Unit =
  sockets.foreach { (playerId, socket) =>
    val view: ClientGameState = state.toClientView(playerId)
    socket.send(Json.encode(view))
  }
```

> `toClientView(playerId)` hides other players' cards. The requesting player gets `myHoleCards`, `myHandCategory`, and `myBestCards`. Everyone else gets `hasCards: true/false` only.

---

## Full example

```scala
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern.*
import poker.actor.GameInstance
import poker.actor.GameInstance.*
import poker.domain.*

given system: ActorSystem[?] = ...
given timeout: Timeout = 3.seconds

val sockets: Map[String, WebSocket] = ...  // playerId -> WebSocket

// 1. Create a table (e.g. when a lobby room is opened)
val table = system.spawn(
  GameInstance("table-1", GameSettings(
    name = "Friday Night",
    smallBlind = 10,
    bigBlind = 20,
    initialChips = 1000,
    isPublic = true,
    maxPlayers = 6
  )),
  "table-1"
)

// 2. A player connects and joins the table
def onPlayerJoin(playerId: String, name: String): Unit =
  table.ask(JoinGame(playerId, name, _)).foreach {
    case GameJoined(_, state) => broadcastState(state)
    case Error(msg)           => sendError(playerId, msg)
  }

// 3. Host starts the game (requires at least 2 players)
def onStartGame(): Unit =
  table.ask(StartGame(_)).foreach {
    case GameStarted(state) => broadcastState(state)
    case Error(msg)         => // e.g. "Not enough players to start"
  }

// 4. Player folds (same pattern for Call, Check, Raise)
def onFold(playerId: String): Unit =
  table.ask(Fold(playerId, _)).foreach {
    case ActionSuccess(state) =>
      broadcastState(state)              // hand ended, next hand pending
    case GameOver(winnerId, state) =>
      broadcastState(state)
      broadcastAll(Json.encode(Map("type" -> "GameOver", "winner" -> winnerId)))
    case Error(msg) =>
      sendError(playerId, msg)
  }

// 5. Raise example (amount is the raise on top of the call)
def onRaise(playerId: String, amount: Int): Unit =
  table.ask(Raise(playerId, amount, _)).foreach {
    case ActionSuccess(state)      => broadcastState(state)
    case GameOver(winnerId, state) =>
      broadcastState(state)
      broadcastAll(Json.encode(Map("type" -> "GameOver", "winner" -> winnerId)))
    case Error(msg)                => sendError(playerId, msg)
  }

// 6. Host updates game settings (only between hands, before first hand)
def onUpdateSettings(): Unit =
  table.ask(UpdateSettings(GameSettings(
    name = "Serious Friday",
    smallBlind = 50,
    bigBlind = 100,
    isPublic = true,
    maxPlayers = 4
  ), _)).foreach {
    case SettingsUpdated(state) => broadcastState(state)
    case Error(msg)             => sendError(hostPlayerId, msg)
  }

// Helpers
def broadcastState(state: GameState): Unit =
  sockets.foreach { (playerId, socket) =>
    socket.send(Json.encode(state.toClientView(playerId)))
  }

def broadcastAll(msg: String): Unit =
  sockets.values.foreach(_.send(msg))

def sendError(playerId: String, msg: String): Unit =
  sockets(playerId).send(Json.encode(Map("type" -> "Error", "msg" -> msg)))
```

> After a hand ends (`ActionSuccess`), players stay at the table – call `StartGame` again for the next hand. When `GameOver` is received, the tournament is over and no more hands can be started.

---

## Error messages

| Situation                     | `msg`                                                     |
| ----------------------------- | --------------------------------------------------------- |
| Not your turn                 | `"Not your turn"`                                         |
| Check when someone raised     | `"Cannot check, must call or raise"`                      |
| Raise more than you have      | `"Not enough chips to raise"`                             |
| Duplicate player ID           | `"Player already joined"`                                 |
| Start with < 2 players        | `"Not enough players to start"`                           |
| Leave during a hand           | `"Cannot leave during a hand. Wait for the hand to end."` |
| UpdateSettings during a hand  | `"Cannot update settings during a hand"`                  |
| UpdateSettings by non-creator | `"Only the game creator can update settings"`             |
