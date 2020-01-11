package learn_akka_basic

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}

import scala.util.Random

object Dispatcher extends App {
  type Command = Any

  class Counter(context: ActorContext[Command])
      extends AbstractBehavior[Command](context) {
    var count = 0

    override def onMessage(msg: Command): Behavior[Command] = {
      count += 1
      context.log.info(
        s"[${context.self.path.toSerializationFormat}] [$count] $msg"
      )
      Thread.sleep(1000)
      this
    }
  }

  object Counter {
    def apply(): Behavior[Command] = Behaviors.setup { ctx =>
      new Counter(ctx)
    }
  }

  object MainSystem {
    sealed trait Command
    case object Start extends Command

    def apply(): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Start) =>
          val actors = for (i <- 1 to 10)
            yield ctx.spawn(Counter(), s"counter_$i", DispatcherSelector.fromConfig("my-dispatcher"))

          val r = new Random()

          ctx.log.info("Start sending message")

          for (i <- 1 to 1000) {
            actors(r.nextInt(10)) ! i
          }

          Behaviors.same
      }
  }

  val system = ActorSystem[MainSystem.Command](MainSystem(), "MainSystem")

  system ! MainSystem.Start
}
