package stores_serialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

object LocalStores extends App {

  class SimplePersistentActor extends PersistentActor with ActorLogging {
    var nMessage = 0

    override def persistenceId: String = "simple-persistent-actor"

    override def receiveCommand: Receive = {
      case "print" =>
        log.info(s"I have persisted $nMessage")
      case "snap" =>
        saveSnapshot(nMessage)
      case SaveSnapshotSuccess(_) =>
        log.info("Save snapshot was successful")
      case SaveSnapshotFailure(_, cause) =>
        log.warning(s"Save snapshot failed: $cause")
      case message => persist(message) { _ =>
        log.info(s"Persisting $message")
        nMessage += 1
      }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        log.info("Recovery done")
      case SnapshotOffer(_, payload: Int) =>
        log.info(s"Recovered snapshot: $payload")
        nMessage = payload
      case message =>
        log.info(s"Recovered: $message")
        nMessage += 1
    }
  }

  val localStoresActorSystem = ActorSystem("localStoresSystem", ConfigFactory.load().getConfig("localStores"))
  val persistentActor = localStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  for (i <- 1 to 10) {
    persistentActor ! s"I love Akka [$i]"
  }

  persistentActor ! "print"
  persistentActor ! "snap"

  for (i <- 11 to 20) {
    persistentActor ! s"I love Akka [$i]"
  }
}
