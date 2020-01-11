package learn_akka_basic

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer}
import akka.actor.typed.{ActorSystem, Behavior}

object StashDemo extends App {

  /**
    * ResourceActor
    * - open => it can receive read/write requests to the resource
    * - otherwise it will postpone all read/write requests until the state is open
    *
    * ResourceActor is close
    * - Open => switch to the open state
    * - Read, Write messages are POSTPONED
    *
    * ResourceActor is open
    * - Read, Write are handled
    * - Close => switch to the closed state
    *
    * [Open, Read, Read, Write]
    * - switch to the open state
    * - read the data
    * - read the data
    * - write the data
    *
    * [Read, Open, Write]
    * - stash Read
    * Stash: [Read]
    * - open => switch to the open state
    * Mailbox: [Read, Write]
    * - read and write are handled
    */
  object ResourceActor {
    sealed trait Command
    case object Open extends Command
    case object Close extends Command
    case object Read extends Command
    case class Write(data: String) extends Command

    def apply(): Behavior[Command] =
      // step 1 - declare withStash
      Behaviors.withStash(100)(buffer => closed(buffer))

    def closed(buffer: StashBuffer[Command]): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Open) =>
          ctx.log.info("Opening resource")
          // step 3 - unstashAll when you switch the message handler
          buffer.unstashAll(open(buffer, ""))

        case (ctx, msg) =>
          ctx.log.info(
            s"Stashing $msg because I can't handle it in the closed state"
          )
          // step 2 - stash away what you can't handle
          buffer.stash(msg)
          Behaviors.same
      }

    def open(buffer: StashBuffer[Command],
             innerData: String): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Read) =>
          // do some actual computation
          ctx.log.info(s"I have read $innerData")
          Behaviors.same
        case (ctx, Write(data)) =>
          ctx.log.info(s"I am writing $data")
          open(buffer, innerData = data)
        case (ctx, Close) =>
          ctx.log.info("Closing resource")
          buffer.unstashAll(closed(buffer))
        case (ctx, msg) =>
          ctx.log.info(
            s"Stashing $msg because I can't handle it in the open state"
          )
          buffer.stash(msg)
          Behaviors.same
      }
  }

  object GuardianSystem {
    sealed trait Command
    case object Start extends Command

    def apply(): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Start) =>
          val resourceActor = ctx.spawnAnonymous(ResourceActor())

          resourceActor ! ResourceActor.Read
          resourceActor ! ResourceActor.Open
          resourceActor ! ResourceActor.Open
          resourceActor ! ResourceActor.Write("I love stash")
          resourceActor ! ResourceActor.Close
          resourceActor ! ResourceActor.Read

          Behaviors.empty
      }
  }

  val system =
    ActorSystem[GuardianSystem.Command](GuardianSystem(), "StashDemo")

  system ! GuardianSystem.Start
}
