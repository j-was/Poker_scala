# PokerScale WebSocket API

All communication happens over a single WebSocket connection at `ws://<host>:<port>/ws`.

Every message is a JSON object with a `"type"` discriminator field.

---

## Connection lifecycle

```
Client connects
  → Client sends   Identify
  ← Server replies Identified        (assigns / confirms playerId)
  → Client sends   CreateGame | JoinGame
  ← Server replies GameCreated | GameJoined
  ↔ Game messages flow…
  Client disconnects (network drop / app close)
  ← Server broadcasts PlayerDisconnected to the room
  → Client reconnects, sends Identify with the same playerId
  ← Server replies Identified, player's seat is restored
  ← Server broadcasts PlayerReconnected to the room
```

> **Persist `playerId` on the client** (e.g. local storage / preferences file).  
> Sending it back in `Identify` on reconnect restores the player's seat seamlessly.

---

## Client → Server

### Identify (send immediately after connecting)

```json
{ "type": "Identify", "name": "John", "playerId": "abc123" }
```

| Field      | Required | Notes                                                                           |
| ---------- | -------- | ------------------------------------------------------------------------------- |
| `name`     | ✅       | Display name shown to other players                                             |
| `playerId` | ❌       | Omit (or `null`) on first connect; send the previously received id to reconnect |

---

### Create a game

```json
{
  "type": "CreateGame",
  "settings": {
    "name": "",
    "smallBlind": 10,
    "bigBlind": 20,
    "initialChips": 1000,
    "isPublic": false,
    "maxPlayers": 9
  }
}
```

| Field        | Required | Default | Notes                                                          |
| ------------ | -------- | ------- | -------------------------------------------------------------- |
| name         | ❌       | `""`    | Custom room name. Empty = auto-generated code. Must be unique. |
| smallBlind   | ❌       | `10`    | Small blind amount in chips                                    |
| bigBlind     | ❌       | `20`    | Big blind amount in chips                                      |
| initialChips | ❌       | `1000`  | Starting chip stack for each player                            |
| isPublic     | ❌       | `false` | When true, game appears in PublicGameList                      |
| maxPlayers   | ❌       | `9`     | Maximum number of players. Joins rejected when reached.        |

> The creator is automatically joined. The server replies with GameCreated containing the room code.  
> **_Private game:_** omit `name` (or set to `""`) and set `isPublic`: false. Share the code manually.  
> **_Public game:_** set `name` to a unique display name and isPublic: true. Other players discover it via `ListPublicGames`.

---

### Join a game

```json
{ "type": "JoinGame", "code": "swift-fox-847" }
```

> Codes are case-insensitive. Not possible after the host has started the game.

---

### Start the game

```json
{ "type": "StartGame", "code": "swift-fox-847" }
```

> Requires at least 2 players. Any player can start (there is no dedicated host role after creation).

---

### Betting actions

```json
{ "type": "Fold",  "code": "swift-fox-847" }
{ "type": "Check", "code": "swift-fox-847" }
{ "type": "Call",  "code": "swift-fox-847" }
{ "type": "Raise", "code": "swift-fox-847", "amount": 50 }
```

> Only works on your turn — the server replies with `Error` otherwise.  
> `amount` in `Raise` is the **raise increment** on top of the current call amount, not the total bet.

---

### Leave a game

```json
{ "type": "LeaveGame", "code": "swift-fox-847" }
```

> Only possible between hands. Rejected mid-hand with an `Error`.

---

### List public games

```json
{ "type": "ListPublicGames" }
```

> Returns a list of all active public games. Only games with `isPublic: true` are included.  
> Private games (created without a name or with `isPublic: false`) are never shown.

---

### Update game settings

```json
{
  "type": "UpdateSettings",
  "code": "swift-fox-847",
  "settings": {
    "name": "New Room Name",
    "smallBlind": 25,
    "bigBlind": 50,
    "initialChips": 2000,
    "isPublic": true,
    "maxPlayers": 6
  }
}
```

> Only the game creator (first player who joined) can update settings.  
> Only possible when `status` is `WaitingForPlayers` (before first hand) or between hands.  
> Rejected with an `Error` during an active hand.

---

### Ping

```json
{ "type": "Ping" }
```

> Use to keep the connection alive. Server replies with `Pong`.

---

## Server → Client

### Identified

Sent in response to `Identify`. The client must save `playerId` for reconnection.

```json
{ "type": "Identified", "playerId": "abc123", "name": "John" }
```

---

### GameCreated

Sent only to the player who created the game.

```json
{
  "type": "GameCreated",
  "code": "swift-fox-847",
  "state": { ...ClientGameState... }
}
```

---

### GameJoined

Sent only to the player who joined.

```json
{
  "type": "GameJoined",
  "code": "swift-fox-847",
  "state": { ...ClientGameState... }
}
```

---

### PlayerJoined

Broadcast to **all other players** already in the room when someone new joins.

```json
{
  "type": "PlayerJoined",
  "code": "swift-fox-847",
  "playerId": "abc123",
  "name": "John"
}
```

---

### PlayerLeft

Broadcast to all players when someone leaves voluntarily (between hands only).

```json
{
  "type": "PlayerLeft",
  "code": "swift-fox-847",
  "playerId": "abc123",
  "name": "John"
}
```

---

### PlayerDisconnected

Broadcast to all players when a WebSocket connection drops unexpectedly.  
The player's seat is preserved; they can reconnect and resume.

```json
{
  "type": "PlayerDisconnected",
  "code": "swift-fox-847",
  "playerId": "abc123",
  "name": "John"
}
```

> If the disconnected player is the current bettor, the server will automatically fold them after **30 seconds** of inactivity so the game never freezes.

---

### PlayerReconnected

Broadcast to all players when a disconnected player comes back.

```json
{
  "type": "PlayerReconnected",
  "code": "swift-fox-847",
  "playerId": "abc123",
  "name": "John"
}
```

---

### PublicGameList

Sent in response to `ListPublicGames`. Contains all active public games.

```json
{
  "type": "PublicGameList",
  "games": [
    {
      "code": "swift-fox-847",
      "name": "Friday Night Poker",
      "playerCount": 3,
      "maxPlayers": 6,
      "smallBlind": 20,
      "bigBlind": 40,
      "isPublic": true
    },
    {
      "code": "calm-wolf-512",
      "name": "High Stakes",
      "playerCount": 1,
      "maxPlayers": 9,
      "smallBlind": 100,
      "bigBlind": 200,
      "isPublic": true
    }
  ]
}
```

| Field         | Meaning                                            |
| ------------- | -------------------------------------------------- |
| `code`        | Room code to use with JoinGame                     |
| `name`        | Custom name set by the creator                     |
| `playerCount` | Number of players currently in the game            |
| `maxPlayers`  | Maximum players allowed (joins rejected when full) |
| `smallBlind`  | Small blind amount                                 |
| `bigBlind`    | Big blind amount                                   |
| `isPublic`    | Always true in this list (public games only)       |

---

### SettingsUpdated

Sent to the player who updated settings, confirming the change.

```json
{
  "type": "SettingsUpdated",
  "code": "swift-fox-847",
  "state": { ...ClientGameState... }
}
```

> The state field contains the full game state with updated settings, so the UI can refresh immediately.

---

### GameStarted

Broadcast to all players when a hand begins. Each player receives a personalised view (their own hole cards only).

```json
{
  "type": "GameStarted",
  "code": "swift-fox-847",
  "state": { ...ClientGameState... }
}
```

---

### StateUpdate

Broadcast to all players after every action. Each player receives a personalised view.

```json
{
  "type": "StateUpdate",
  "code": "swift-fox-847",
  "state": { ...ClientGameState... }
}
```

---

### GameOver

Broadcast once when the tournament ends (one player holds all chips). A final `StateUpdate` with `status: "Finished"` follows so the UI can show final chip counts.

```json
{
  "type": "GameOver",
  "code": "swift-fox-847",
  "winnerId": "abc123",
  "winnerName": "John",
  "state": { ...ClientGameState... }
}
```

---

### Error

Sent only to the player whose action triggered the error.

```json
{ "type": "Error", "message": "Cannot check, must call or raise" }
```

---

### Pong

```json
{ "type": "Pong" }
```

---

## ClientGameState object

Included in `GameCreated`, `GameJoined`, `GameStarted`, `StateUpdate`, and `GameOver`.  
Each player receives a personalised copy — other players' hole cards are never sent.

```json
{
  "id": "swift-fox-847",
  "status": "Playing",

  "settings": {
    "smallBlind": 10,
    "bigBlind": 20,
    "initialChips": 1000
  },

  "board": {
    "street": "Flop",
    "cards": [
      { "rank": "A", "suit": "♠" },
      { "rank": "K", "suit": "♥" },
      { "rank": "Q", "suit": "♦" }
    ]
  },

  "players": [
    {
      "id": "abc123",
      "name": "John",
      "chips": 800,
      "currentBet": 200,
      "isActive": true,
      "hasActed": true,
      "hasCards": true
    }
  ],

  "pot": 600,
  "dealerIndex": 0,
  "currentPlayerIndex": 1,
  "currentHighestBet": 200,

  "myHoleCards": {
    "c1": { "rank": "J", "suit": "♠" },
    "c2": { "rank": "T", "suit": "♠" }
  },
  "myHandCategory": "OnePair",
  "myBestCards": [
    { "rank": "J", "suit": "♠" },
    { "rank": "J", "suit": "♥" },
    { "rank": "A", "suit": "♣" },
    { "rank": "K", "suit": "♦" },
    { "rank": "Q", "suit": "♠" }
  ]
}
```

### Player fields

| Field      | Meaning                                                    |
| ---------- | ---------------------------------------------------------- |
| `isActive` | `false` = player folded or is sitting out (no chips)       |
| `hasActed` | whether this player has acted in the current betting round |
| `hasCards` | `true` → render card backs; `false` → empty seat           |

### Board fields

| Field    | Meaning                                                |
| -------- | ------------------------------------------------------ |
| `street` | Current street (see enum below)                        |
| `cards`  | Community cards dealt so far; empty array on `PreFlop` |

### My-player fields

| Field                | Meaning                                                           |
| -------------------- | ----------------------------------------------------------------- |
| `myHoleCards`        | Your 2 hole cards. `null` when you have no cards                  |
| `myHandCategory`     | Your current best hand category. `null` when you have no cards    |
| `myBestCards`        | The 5 cards forming your best hand. `null` when you have no cards |
| `dealerIndex`        | Index into `players` of the dealer-button holder                  |
| `currentPlayerIndex` | Index into `players` of the player whose turn it is               |

---

## Enum reference

### `status`

`WaitingForPlayers` · `Playing` · `Finished`

### `board.street`

`PreFlop` · `Flop` · `Turn` · `River`

### `myHandCategory`

`HighCard` · `OnePair` · `TwoPair` · `ThreeOfAKind` · `Straight` · `Flush` · `FullHouse` · `FourOfAKind` · `StraightFlush`

### `rank` (toString representation)

`2` `3` `4` `5` `6` `7` `8` `9` `T` `J` `Q` `K` `A`

### `suit` (toString representation)

`♣` · `♦` · `♥` · `♠`

---

## Error reference

| Message                                                                                                  | Cause                                                          |
| -------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `"Not your turn"`                                                                                        | Action sent when it isn't your turn                            |
| `"Cannot check, must call or raise"`                                                                     | Check attempted when there is an outstanding bet               |
| `"Not enough chips to raise"`                                                                            | Raise amount exceeds your stack                                |
| `"You are not in a game"`                                                                                | Action sent before joining a game                              |
| `"Game not found. Check the code and try again."`                                                        | Unknown or expired room code                                   |
| `"Player already joined"`                                                                                | Duplicate join with the same playerId                          |
| `"Cannot join after the game has started"`                                                               | Join attempted after `StartGame`                               |
| `"Not enough players to start"`                                                                          | StartGame with fewer than 2 players                            |
| `"Game already in progress"`                                                                             | StartGame sent during an active hand                           |
| `"Cannot leave during a hand. Wait for the hand to end."`                                                | LeaveGame mid-hand                                             |
| `"Tournament is over"`                                                                                   | StartGame after `status: Finished`                             |
| `"Game name already exists"`                                                                             | CreateGame with a name that is already taken by an active game |
| `"Failed to list public games"`                                                                          | Server error while fetching public game list                   |
| `"Failed to update settings"`                                                                            | Settings update rejected or timed out                          |
| `"You already host a game with code 'dark-wolf-456'. Your game needs to end before creating a new one."` | One person can host only one game at a time                    |

# Usage patterns

## Public vs Private games

### Creating a private game

```json
// Client sends
{ "type": "CreateGame", "settings": { "smallBlind": 10, "bigBlind": 20, "initialChips": 1000 } }

// Server replies with auto-generated code
{ "type": "GameCreated", "code": "swift-fox-847", "state": { ... } }

// Creator shares "swift-fox-847" with friends via chat/message
```

---

### Creating a public game

```json
// Client sends
{
  "type": "CreateGame",
  "settings": {
    "name": "Casual Friday",
    "smallBlind": 20,
    "bigBlind": 40,
    "initialChips": 2000,
    "isPublic": true,
    "maxPlayers": 6
  }
}

// Server replies with code
{ "type": "GameCreated", "code": "brave-hawk-342", "state": { ... } }
```

---

### Browsing and joining public games

```json
// Any player sends
{ "type": "ListPublicGames" }

// Server replies
{
  "type": "PublicGameList",
  "games": [
    { "code": "brave-hawk-342", "name": "Casual Friday", "playerCount": 2, "maxPlayers": 6, ... },
    { "code": "calm-wolf-512", "name": "High Stakes", "playerCount": 5, "maxPlayers": 9, ... }
  ]
}

// Player picks a game and joins normally
{ "type": "JoinGame", "code": "brave-hawk-342" }
```

---

### Updating settings between hands

```json
// Only the creator (first player) can do this
// Must be in WaitingForPlayers phase or between hands

{
  "type": "UpdateSettings",
  "code": "brave-hawk-342",
  "settings": {
    "name": "Serious Friday",
    "smallBlind": 50,
    "bigBlind": 100,
    "initialChips": 5000,
    "isPublic": true,
    "maxPlayers": 4
  }
}

// Server confirms
{ "type": "SettingsUpdated", "code": "brave-hawk-342", "state": { ... } }
```

> All fields in `settings` are optional — only send the ones you want to change.  
> The `name` must still be unique if changed.

---

### Full connection flow with public games

```text
Client connects
  → Client sends   Identify
  ← Server replies Identified
  → Client sends   ListPublicGames
  ← Server replies PublicGameList
  → Client sends   JoinGame with code from list
  ← Server replies GameJoined with full state
  ↔ Game messages flow…
```

> You can skip ListPublicGames if the player already knows the code (e.g. shared privately).  
> The flow for private games is unchanged — just CreateGame / JoinGame as before.
