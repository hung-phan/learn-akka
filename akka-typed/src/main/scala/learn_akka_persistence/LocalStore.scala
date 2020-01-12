package learn_akka_persistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

object LocalStore extends App {
  sealed trait Command
  case object Start extends Command

  object MainSystem {
    def apply(): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Start) =>
          import SimplePersistentActor._

          val persistentActor =
            ctx.spawn(SimplePersistentActor(), "simplePersistentActor")

          for (i <- 1 to 10) {
            persistentActor ! Add(s"I love Akka [$i]")
          }

          persistentActor ! Print

          for (i <- 11 to 20) {
            persistentActor ! Add(s"I love Akka [$i]")
          }

          Behaviors.empty
      }
  }

  val system = ActorSystem[Command](
    MainSystem(),
    "localStoreSystem",
    ConfigFactory
      .load("learn_akka_persistence/local_store.conf")
  )

  system ! Start
}
