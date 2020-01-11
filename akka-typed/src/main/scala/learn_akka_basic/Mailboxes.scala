package learn_akka_basic

import akka.actor.ActorSystem.Settings
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, MailboxSelector}
import akka.dispatch.{
  ControlMessage,
  PriorityGenerator,
  UnboundedPriorityMailbox
}
import com.typesafe.config.Config

object Mailboxes extends App {
  class PriorityMailbox(settings: Settings, cfg: Config)
      extends UnboundedPriorityMailbox(PriorityGenerator {
        case SimpleActor.Message(msg) if msg.startsWith("[P0]") => 0
        case SimpleActor.Message(msg) if msg.startsWith("[P1]") => 1
        case SimpleActor.Message(msg) if msg.startsWith("[P2]") => 2
        case SimpleActor.Message(msg) if msg.startsWith("[P3]") => 3
        case _                                                  => 4
      })

  type Command = Any

  object SimpleActor {
    sealed trait Command
    case class Message(msg: String) extends Command
    case object ManagementTicket extends Command with ControlMessage

    def apply(): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Message(msg)) =>
          ctx.log.info(msg)
          Behaviors.same
        case (ctx, ManagementTicket) =>
          ctx.log.info(s"Receive: $ManagementTicket")
          Behaviors.same
      }
  }

  object MainSystem {
    sealed trait Command
    case object Start extends Command

    def apply(): Behavior[Command] =
      Behaviors.receivePartial {
        case (ctx, Start) =>
          val supportTicketLogger = ctx.spawn(
            SimpleActor(),
            "support-ticket-logger",
            MailboxSelector.fromConfig("support-ticket-dispatcher")
          )
          supportTicketLogger ! SimpleActor.Message("[P3] Message 1")
          supportTicketLogger ! SimpleActor.Message("[P0] Message 2")
          supportTicketLogger ! SimpleActor.Message("[P1] Message 3")

          val controlAwareActor = ctx.spawn(
            SimpleActor(),
            "control-aware-actor",
            MailboxSelector.fromConfig("control-mailbox")
          )

          controlAwareActor ! SimpleActor.Message(
            "[P3] this thing would be nice to have"
          )
          controlAwareActor ! SimpleActor.Message(
            "[P0] this needs to be solved NOW!"
          )
          controlAwareActor ! SimpleActor.Message(
            "[P1] do this when you have the time"
          )
          controlAwareActor ! SimpleActor.ManagementTicket

          Behaviors.empty
      }
  }

  val system = ActorSystem[MainSystem.Command](MainSystem(), "MainSystem")

  system ! MainSystem.Start
}
