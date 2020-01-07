package learn_akka_remoting

import akka.actor.{Actor, ActorLogging}

class SimpleActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case m => log.info(s"Receive $m from ${sender()}")
  }
}
