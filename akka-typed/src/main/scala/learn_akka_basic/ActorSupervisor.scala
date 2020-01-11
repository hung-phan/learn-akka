package learn_akka_basic

import java.io.File

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object ActorSupervisor extends App {
  sealed trait FileCommand
  case object ReadFile extends FileCommand

  def FileBasedPersistentActor(): Behavior[FileCommand] =
    Behaviors.setup { ctx =>
      ctx.log.info("Persistent actor starting")
      var dataSource: Source = null

      Behaviors
        .receiveMessagePartial[FileCommand] {
          case ReadFile =>
            if (dataSource == null)
              dataSource = Source.fromFile(
                new File("src/main/resources/testfiles/important_data.txt")
              )

            ctx.log.info(
              s"I've just read some IMPORTANT data: ${dataSource.getLines().toList}"
            )
            Behaviors.same
        }
        .receiveSignal {
          case (ctx, PostStop) =>
            ctx.log.info("Persistent actor has stopped")
            Behaviors.ignore
          case (ctx, PreRestart) =>
            ctx.log.warn("Persistent actor restarting")
            Behaviors.same
        }
    }

  def EagerFBActor(): Behavior[FileCommand] =
    Behaviors.setup { ctx =>
      ctx.log.info("Persistent actor starting")

      // throw Error
      Source.fromFile(
        new File("src/main/resources/testfiles/important_data.txt")
      )

      Behaviors
        .receiveSignal {
          case (ctx, PostStop) =>
            ctx.log.info("Persistent actor has stopped")
            Behaviors.ignore
          case (ctx, PreRestart) =>
            ctx.log.warn("Persistent actor restarting")
            Behaviors.same
        }
    }

  sealed trait MainApplicationCommand
  case object Start extends MainApplicationCommand

  def SimpleSystem(): Behavior[MainApplicationCommand] =
    Behaviors.receivePartial {
      case (ctx, Start) =>
        val simpleActor = ctx.spawn(FileBasedPersistentActor(), "simpleActor")

        simpleActor ! ReadFile

        Behaviors.empty
    }

  def SystemWithSupervisor(): Behavior[MainApplicationCommand] =
    Behaviors.receivePartial {
      case (ctx, Start) =>
        val simpleActor = ctx.spawn(
          Behaviors
            .supervise(FileBasedPersistentActor)
            .onFailure[Exception](SupervisorStrategy.restart),
          "supervisedSimpleActor"
        )

        simpleActor ! ReadFile

        Behaviors.empty
    }

  def SystemWithAdvanceSupervisor(): Behavior[MainApplicationCommand] =
    Behaviors.receivePartial {
      case (ctx, Start) =>
        val simpleActor = ctx.spawn(
          Behaviors
            .supervise(EagerFBActor)
            .onFailure[Exception](
              SupervisorStrategy.restartWithBackoff(3 seconds, 30 seconds, 0.2)
            ),
          "supervisedSimpleActor"
        )

        simpleActor ! ReadFile

        Behaviors.empty
    }

//  val system: ActorSystem[Start.type] =
//    ActorSystem(SimpleSystem(), "SimpleSystem")
//  val system: ActorSystem[Start.type] =
//    ActorSystem(SystemWithSupervisor(), "SystemWithSupervisor")
  val system: ActorSystem[Start.type] =
    ActorSystem(SystemWithAdvanceSupervisor(), "SystemWithAdvanceSupervisor")

  system ! Start
}
