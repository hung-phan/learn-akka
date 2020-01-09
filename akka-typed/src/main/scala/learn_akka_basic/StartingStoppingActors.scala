package learn_akka_basic

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object MainLogic {
  sealed trait ParentCommand
  final case class StartChild(name: String) extends ParentCommand
  final case class StopChild(name: String) extends ParentCommand
  final case class SpecialMessage(text: String) extends ParentCommand
  final case object Stop extends ParentCommand

  type ChildCommand = Any

  val ChildServiceKey = ServiceKey[ChildCommand]("ChildService")

  def Child(): Behavior[ChildCommand] =
    Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(
        ChildServiceKey,
        context.self
      )

      Behaviors.receiveMessage { message =>
        context.log.info(message.toString)
        Behaviors.same
      }
    }

  def Parent(
    children: Map[String, ActorRef[ChildCommand]]
  ): Behavior[ParentCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case SpecialMessage(text) =>
          context.log.info(s"Receive special message: $text")
          Behaviors.same
        case StartChild(name) =>
          context.log.info(s"Starting child $name")

          Parent(children + (name -> context.spawn(Child(), name)))
        case StopChild(name) =>
          context.log.info(s"Stopping child with the name $name")

          children.get(name).foreach(context.stop(_))

          Behaviors.same
        case Stop =>
          context.log.info("Stopping myself")
          Behaviors.stopped
      }
    }
}

object MainSystem {
  import MainLogic._

  sealed trait Command
  final case object Start extends Command
  final case class CreateChild(name: String) extends Command
  private case class ListingResponse(listing: Receptionist.Listing)
      extends Command

  def exampleWithCustomMessage(): Behavior[Command] =
    Behaviors.setup { context =>
      val listingAdapter =
        context.messageAdapter[Receptionist.Listing](ListingResponse)

      context.system.receptionist ! Receptionist.Subscribe(
        ChildServiceKey,
        listingAdapter
      )

      Behaviors.receiveMessagePartial {
        case Start =>
          val parent =
            context.spawnAnonymous[ParentCommand](Parent(Map.empty))

          parent ! StartChild("child1")
          parent ! StartChild("child2")

          Behaviors.receiveMessagePartial {
            case ListingResponse(ChildServiceKey.Listing(listing)) =>
              if (listing.size == 2) {
                listing.foreach { ref =>
                  ref ! "hi kid!"
                }

                parent ! StopChild("child1")
                parent ! Stop

                for (_ <- 1 to 10)
                  parent ! SpecialMessage("parent, are you still there?")
                for (i <- 1 to 100)
                  listing.last ! s"[$i] last kid, are you still there?"
              }

              Behaviors.same
          }
      }
    }

  def exampleWithWatchActor(): Behavior[Command] =
    Behaviors
      .receivePartial[Command] {
        case (context, CreateChild(name)) =>
          val child = context.spawn(Child(), name)

          context.log.info(s"Started and watching child $name")

          context.system.scheduler.scheduleOnce(3 seconds, () => {
            context.stop(child)
          })
          context.watch(child)

          Behaviors.same
      }
      .receiveSignal {
        case (context, Terminated(ref)) =>
          context.log.info("Job stopped: {}", ref.path.toSerializationFormat)
          Behaviors.same
      }
}

object StartingStoppingActors extends App {
  import MainSystem._

  val system1: ActorSystem[Command] =
    ActorSystem(exampleWithCustomMessage(), "StoppingActorsDemoExample1")

  system1 ! Start

  val system2: ActorSystem[Command] =
    ActorSystem(exampleWithWatchActor(), "StoppingActorsDemoExample2")

  system2 ! CreateChild("watchedChild")
}
