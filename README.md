# PokerScale

Play Texas Hold'em with friends — host private games or join public tables. Playing each game with virtual money, no sign up required.

A full-stack poker application built in Scala. Features a desktop client made with ScalaFX and a Pekko-powered server with WebSocket communication.

Developed for the **"Programming in Scala"** course at the **Faculty of Mathematics and Information Science (MiNI)**, Warsaw University of Technology.

## Team

| Member | Role |
|--------|------|
| [Andrzej Wrzesiński](https://github.com/ondrey-16) | Desktop app |
| [Artur Szabelski](https://github.com/Artur112233) | Desktop frontend |
| [Fryderyk Wolny](https://github.com/Frycek1) | Poker engine |
| [Jerzy Wąsiewicz](https://github.com/j-was) | Backend server |

## Features

- **Texas Hold'em** with full betting rounds (pre-flop, flop, turn, river)
- **Multiplayer** over WebSocket — real-time state updates
- **Private games** — create a room, share the code with friends
- **Public games** — browse and join open tables
- **Reconnection** — drop out and rejoin without losing your seat
- **Auto-fold** — inactive players are folded after 30s so the game never stalls
- **Game settings** - adjust the blinds and initial chips to your liking

## API

**Full API documentation**: [`api.md`](./docs/api.md)  
Engine usage guide: [`engine.md`](./docs/engine.md)
