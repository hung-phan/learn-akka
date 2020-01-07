package learn_akka_basic

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")

  import system.dispatcher

  system.scheduler.scheduleOnce(5 second) {
    simpleActor ! "reminder"
  }

  val routine = system.scheduler.schedule(1 second, 2 seconds) {
    simpleActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel()
  }

  /**
   * Exercise: implement a self-closing actor
   *
   * - if the actor receives a message (anything), you have 1 second to send it another message
   *  - if the time window expires, the actor will stop itself
   *  - if you send another message, the time window is reset
   */
  class TimerActor extends Actor with ActorLogging {
    var scheduler: Cancellable = null

    override def preStart(): Unit = {
      log.info(s"TimerActor ${self.path} starting")
    }

    override def postStop(): Unit = {
      log.info(s"TimerActor ${self.path} stopping")
    }

    override def receive: Receive = {
      case message =>
        if (scheduler != null) {
          scheduler.cancel()
        }

        log.info(s"TimerActor ${self.path}: ${message.toString}")

        scheduler = context.system.scheduler.scheduleOnce(1 second) {
          context.stop(self)
        }
    }
  }

  val timerActor = system.actorOf(Props[TimerActor], "timerActor")

  val anotherRoutine = system.scheduler.schedule(0 second, 500 milliseconds) {
    timerActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(5 seconds) {
    anotherRoutine.cancel()
  }

  case object TimerKey

  case object Start

  case object Reminder

  case object Stop

  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")

        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)

      case Reminder =>
        log.info("I am alive")

      case Stop =>
        log.warning("stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "timerHeartbeatActor")

  system.scheduler.scheduleOnce(5 seconds) {
    timerHeartbeatActor ! Stop
  }
}
