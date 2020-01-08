package learn_akka_basic

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, Props}

object Playground extends App {
  object WordCounterActor {
    def apply(): Behavior[String] = counter(0)

    private def counter(total: Int): Behavior[String] =
      Behaviors.receive[String] { (context, message) =>
        context.log.info(s"Receive: $message")

        if (message == "STOP") {
          Behaviors.stopped
        } else {
          val newTotal = total + message.split(" ").length

          context.log.info(s"New count: $newTotal")

          counter(newTotal)
        }
      }
  }

  object Person {
    def apply(name: String): Behavior[String] =
      Behaviors.receive { (context, message) =>
        context.log.info(s"Hello, I receive '$message' from $name")
        Behaviors.same
      }
  }

  object MainSystem {
    final case object Start

    def apply(): Behavior[Start.type] =
      Behaviors.setup { context =>
        val wordCounter1 = context.spawn(WordCounterActor(), "wordCounter1")
        val wordCounter2 = context.spawn(WordCounterActor(), "wordCounter2")
        val person = context.spawn(Person("HungPhan"), "person")

        Behaviors.receiveMessage { message =>
          wordCounter1 ! "How are you doing?"
          wordCounter2 ! "Testing"

          person ! "hi"
          Behaviors.same
        }
      }
  }

  val system: ActorSystem[MainSystem.Start.type] =
    ActorSystem(MainSystem(), "Playground")

  system ! MainSystem.Start
  system ! MainSystem.Start
}
