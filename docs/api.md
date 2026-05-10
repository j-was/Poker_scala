## Frontend → Server

### Join a table
```json
{ "type": "JoinGame", "playerId": "abc123", "name": "John" }
```
> Starting chips are set by the server based on tournament settings

### Start the game
```json
{ "type": "StartGame" }
```
> At least 2 players need to be at the table.

### Betting actions
```json
{ "type": "Check", "playerId": "abc123" }
{ "type": "Call",  "playerId": "abc123" }
{ "type": "Fold",  "playerId": "abc123" }
{ "type": "Raise", "playerId": "abc123", "amount": 50 }
```
> Only works on your turn – otherwise you'll get an `Error` back.

### Leave the table
```json
{ "type": "LeaveGame", "playerId": "abc123" }
```
> Only possible between hands.

---

## Server → Frontend

### Game state update

After every action the server pushes a new state to all players. Data should be scrubbed so that each client only receives their own hidden data.

```json
{
  "type": "GameStateUpdate",
  "data": {
    "id": "table-1",
    "status": "Playing",

    "settings": {
      "smallBlind": 10,
      "bigBlind": 20,
      "initialChips": 1000
    },

    "board": {
      "phase": "Flop",
      "cards": [
        { "rank": "Ace",   "suit": "Spades"   },
        { "rank": "King",  "suit": "Hearts"   },
        { "rank": "Queen", "suit": "Diamonds" }
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
      "c1": { "rank": "Jack", "suit": "Spades" },
      "c2": { "rank": "Ten",  "suit": "Spades" }
    },
    "myHandCategory": "OnePair",
    "myBestCards": [
      { "rank": "Jack",  "suit": "Spades"   },
      { "rank": "Jack",  "suit": "Hearts"   },
      { "rank": "Ace",   "suit": "Clubs"    },
      { "rank": "King",  "suit": "Diamonds" },
      { "rank": "Queen", "suit": "Spades"   }
    ]
  }
}
```

Field reference:

| Field | What it means |
|-------|--------------|
| `isActive` | `false` = player folded or is out of chips |
| `hasActed` | whether the player already acted this betting round |
| `hasCards` | `true` = render card backs; `false` = empty seat |
| `dealerIndex` | who has the dealer button (index into `players`) |
| `currentPlayerIndex` | whose turn it is (index into `players`) |
| `myHoleCards` | your 2 hole cards – `null` for everyone else |
| `myHandCategory` | your current best hand – `null` if you have no cards |
| `myBestCards` | the 5 cards that make up your best hand |

### Error

```json
{ "type": "Error", "msg": "Cannot check, must call or raise" }
```

### Game over

Sent once when the tournament ends (one player has all the chips).

```json
{ "type": "GameOver", "winner": "abc123" }
```

> After receiving `GameOver` the server will also push a final `GameStateUpdate` with `status: "Finished"` so the UI can show the final chip counts.

---

## Enum values

**`status`:** `WaitingForPlayers` · `Playing` · `Finished`

**`board.phase`:** `PreFlop` · `Flop` · `Turn` · `River`

**`myHandCategory`:** `HighCard` · `OnePair` · `TwoPair` · `ThreeOfAKind` · `Straight` · `Flush` · `FullHouse` · `FourOfAKind` · `StraightFlush`

**`rank`:** `Two` `Three` `Four` `Five` `Six` `Seven` `Eight` `Nine` `Ten` `Jack` `Queen` `King` `Ace`

**`suit`:** `Spades` · `Hearts` · `Diamonds` · `Clubs`
