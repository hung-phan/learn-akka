package learn_akka_persistence

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{
  Effect,
  EventSourcedBehavior,
  RetentionCriteria
}

import scala.concurrent.duration._
import scala.language.postfixOps

object SimplePersistentActor {
  sealed trait Command
  case class Add(data: Any) extends Command
  case object Print extends Command

  sealed trait Event
  case class Added(data: Any) extends Event

  final case class State(nMessage: Int)

  def onCommand(ctx: ActorContext[Command],
                state: State,
                command: Command): Effect[Event, State] = {
    command match {
      case Print =>
        ctx.log.info(s"I have persisted ${state.nMessage}")
        Effect.none

      case Add(data) =>
        ctx.log.info(s"Persisting $data")

        Effect.persist(Added(data))
    }
  }

  def applyEvent(ctx: ActorContext[Command],
                 state: State,
                 event: Event): State = {
    event match {
      case _: Added =>
        ctx.log.info(s"Restore state $state")

        state.copy(nMessage = state.nMessage + 1)
    }
  }

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      EventSourcedBehavior[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId("simple-persistent-actor"),
        emptyState = State(0),
        commandHandler = (state, command) => onCommand(ctx, state, command),
        eventHandler = (state, event) => applyEvent(ctx, state, event)
      ).onPersistFailure(
          SupervisorStrategy.restartWithBackoff(1 second, 30 seconds, 0.2)
        )
        .withRetention(RetentionCriteria.snapshotEvery(10, 2))
    }
}
