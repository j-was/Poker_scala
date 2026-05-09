package poker.actor

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import poker.domain.*

object GameInstance:

  sealed trait Command
  
  case class JoinGame(playerId: String, name: String, replyTo: ActorRef[Response]) extends Command
  case class StartGame(replyTo: ActorRef[Response]) extends Command
  case class Fold(playerId: String, replyTo: ActorRef[Response]) extends Command
  case class Call(playerId: String, replyTo: ActorRef[Response]) extends Command
  case class Check(playerId: String, replyTo: ActorRef[Response]) extends Command
  case class Raise(playerId: String, amount: Int, replyTo: ActorRef[Response]) extends Command
  case class LeaveGame(playerId: String, replyTo: ActorRef[Response]) extends Command
  
  case class GetState(replyTo: ActorRef[GameState]) extends Command

  sealed trait Response
  case class GameJoined(playerId: String, state: GameState) extends Response
  case class GameStarted(state: GameState) extends Response
  case class ActionSuccess(state: GameState) extends Response
  case class Error(msg: String) extends Response

  def apply(id: String, settings: GameSettings = GameSettings()): Behavior[Command] =
    Behaviors.setup { context =>
      waitingForPlayers(GameState(id = id, settings = settings))
    }

  private def waitingForPlayers(state: GameState): Behavior[Command] = Behaviors.receiveMessage {
    case JoinGame(playerId, name, replyTo) =>
      if state.players.exists(_.id == playerId) then
        replyTo ! Error("Player already joined")
        Behaviors.same
      else
        val newPlayer = Player(playerId, name, state.settings.initialChips)
        val newState = state.copy(players = state.players :+ newPlayer)
        replyTo ! GameJoined(playerId, newState)
        waitingForPlayers(newState)

    case StartGame(replyTo) =>
      if state.players.size < 2 then
        replyTo ! Error("Not enough players to start")
        Behaviors.same
      else
        val newState = PokerEngine.startNewHand(state)
        replyTo ! GameStarted(newState)
        playing(newState)

    case GetState(replyTo) =>
      replyTo ! state
      Behaviors.same

    case LeaveGame(playerId, replyTo) =>
      val newState = state.copy(players = state.players.filterNot(_.id == playerId))
      replyTo ! ActionSuccess(newState)
      waitingForPlayers(newState)

    case _ =>
      Behaviors.unhandled
  }


  private def playing(state: GameState): Behavior[Command] = Behaviors.receiveMessage {
    case GetState(replyTo) =>
      replyTo ! state
      Behaviors.same

    case fold: Fold =>
      handleAction(state, fold.playerId, fold.replyTo) { (p, st) =>
        st.updatePlayer(p.copy(isActive = false, hasActed = true))
      }

    case check: Check =>
      handleAction(state, check.playerId, check.replyTo) { (p, st) =>
        if p.currentBet < st.currentHighestBet then
          throw new IllegalArgumentException("Cannot check, must call or raise")
        st.updatePlayer(p.copy(hasActed = true))
      }

    case call: Call =>
      handleAction(state, call.playerId, call.replyTo) { (p, st) =>
        val amountToCall = st.currentHighestBet - p.currentBet
        val actualCall = math.min(amountToCall, p.chips)
        st.updatePlayer(p.copy(
          chips = p.chips - actualCall,
          currentBet = p.currentBet + actualCall,
          hasActed = true
        ))
      }

    case raise: Raise =>
      handleAction(state, raise.playerId, raise.replyTo) { (p, st) =>
        val amountToCall = st.currentHighestBet - p.currentBet
        val totalAmount = amountToCall + raise.amount
        if totalAmount > p.chips then
          throw new IllegalArgumentException("Not enough chips to raise")
        st.updatePlayer(p.copy(
          chips = p.chips - totalAmount,
          currentBet = p.currentBet + totalAmount,
          hasActed = true
        )).copy(currentHighestBet = st.currentHighestBet + raise.amount)
      }

    case StartGame(replyTo) =>
        replyTo ! Error("Game already in progress")
        Behaviors.same
        
    case LeaveGame(playerId, replyTo) =>
      replyTo ! Error("Cannot leave during a hand. Wait for the hand to end.")
      Behaviors.same

    case _ => Behaviors.unhandled
  }

  private def handleAction(
    state: GameState, 
    playerId: String, 
    replyTo: ActorRef[Response]
  )(action: (Player, GameState) => GameState): Behavior[Command] =
    state.currentPlayer.filter(_.id == playerId) match
      case None =>
        replyTo ! Error("Not your turn")
        Behaviors.same
      case Some(currentPlayer) =>
        try
          val stAfterAction = action(currentPlayer, state)
          val stWithNextTurn = stAfterAction.advanceTurn()

          val finalState = if stWithNextTurn.isBettingRoundOver then
            PokerEngine.advancePhase(stWithNextTurn)
          else
            stWithNextTurn

          replyTo ! ActionSuccess(finalState)

          if finalState.status == GameStatus.WaitingForPlayers || finalState.status == GameStatus.Finished then
            waitingForPlayers(finalState)
          else
            playing(finalState)
        catch
          case e: Exception =>
            replyTo ! Error(e.getMessage)
            Behaviors.same

