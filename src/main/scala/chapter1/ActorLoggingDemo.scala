package chapter1

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)

    override def receive: Receive = {
      case (a, b) => logger.info("Two things: {} and {}", a, b)
      case message => logger.info(message.toString)
    }
  }

  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b)
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("ActorLoggingDemo")
  val actor1 = system.actorOf(Props[SimpleActorWithExplicitLogger])
  val actor2 = system.actorOf(Props[ActorWithLogging])

  actor1 ! "Logging a simple message"
  actor2 ! "Logging a simple message"

  actor2 ! (42, 65)
}
