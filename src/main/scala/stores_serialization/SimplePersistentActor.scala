package stores_serialization

import akka.actor.ActorLogging
import akka.persistence.{DeleteMessagesSuccess, DeleteSnapshotFailure, DeleteSnapshotsSuccess, PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer, SnapshotSelectionCriteria}

class SimplePersistentActor extends PersistentActor with ActorLogging {
  var nMessage = 0

  override def persistenceId: String = "simple-persistent-actor"

  override def receiveCommand: Receive = {
    case "print" =>
      log.info(s"I have persisted $nMessage")
    case "snap" =>
      saveSnapshot(nMessage)
    case SaveSnapshotSuccess(metadata) =>
      log.info("Save snapshot was successful")

      deleteSnapshots(SnapshotSelectionCriteria.create(metadata.sequenceNr - 1, metadata.timestamp))
      deleteMessages(metadata.sequenceNr)
    case SaveSnapshotFailure(_, cause) =>
      log.warning(s"Save snapshot failed: $cause")
    case DeleteSnapshotsSuccess(_) =>
      log.info("Delete old snapshot was successful")
    case DeleteMessagesSuccess(_) =>
      log.info("Delete old messages were successful")
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
