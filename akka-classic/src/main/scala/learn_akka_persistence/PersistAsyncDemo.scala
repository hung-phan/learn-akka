package learn_akka_persistence

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory

object PersistAsyncDemo extends App {

  case class Command(contents: String)

  case class Event(contents: String)

  class CriticalStreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case message =>
        log.info(s"Recovered: $message")
    }

    override def receiveCommand: Receive = {
      case Command(contents) =>
        eventAggregator ! s"Processing $contents"

        persistAsync(Event(contents)) { e =>
          eventAggregator ! e
        }

        // some actual computation
        val processedContents = contents + "_processed"

        persistAsync(Event(processedContents)) { e =>
          eventAggregator ! e
        }
    }

    override def persistenceId: String = "critical-stream-processor"

  }

  object CriticalStreamProcessor {
    def props(eventAggregator: ActorRef) = Props(new CriticalStreamProcessor(eventAggregator))
  }

  class EventAggregator extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("PersistAsyncDemo", ConfigFactory.load().getConfig("learn_akka_persistence"))
  val eventAggregator = system.actorOf(Props[EventAggregator], "eventAggregator")
  val streamProcessor = system.actorOf(CriticalStreamProcessor.props(eventAggregator), "streamProcessor")

  streamProcessor ! Command("command1")
  streamProcessor ! Command("command2")

  /**
   * persistAsync vs persist
   * - perf: high-throughput environments
   *
   * persist vs persisAsync
   * - ordering guarantees
   */
}
