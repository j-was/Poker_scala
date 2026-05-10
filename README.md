# Poker in Scala

App to play poker with your friends.

This project is being developed for the **"Programming in Scala"** course at the Faculty of Mathematics and Information Science (MiNI), Warsaw University of Technology.

- [Andrzej Wrzesiński](https://github.com/ondrey-16)
- [Artur Szabelski](https://github.com/Artur112233)
- [Fryderyk Wolny](https://github.com/Frycek1)
- [Jerzy Wąsiewicz](https://github.com/j-was)

---

## Modules

| Folder | Who | Tech |
|--------|-----|------|
| `src/` | Fryderyk – game engine | Scala 3, Apache Pekko |
| `server/` | Jerzy – WebSocket server | Scala 3, Pekko HTTP |
| `frontend/` | Andrzej, Artur – UI | TBD |

## Docs

- [`docs/engine.md`](docs/engine.md) – how to use the game engine (for Jerzy)
- [`docs/api.md`](docs/api.md) – WebSocket JSON contract (for frontend)

## Quick start

```bash
sbt test    # run all tests (30 passing)
sbt run     # CLI demo
```

Requires JDK 17+ and SBT 1.x.
