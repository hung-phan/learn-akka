package learn_akka_basic

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object ActorTypedObjectOrientedStyle {
  class WordCounterActor(context: ActorContext[String])
      extends AbstractBehavior[String](context) {
    var total = 0

    override def onMessage(msg: String): Behavior[String] = {
      context.log.info(s"Receive: $msg")

      msg match {
        case "STOP" =>
          Behaviors.stopped
        case _ =>
          total += msg.split(" ").length
          context.log.info(s"New count: $total")
          this
      }
    }
  }
}
