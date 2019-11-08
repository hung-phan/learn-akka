package chapter2

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery, RecoveryCompleted, SnapshotSelectionCriteria}
import com.typesafe.config.ConfigFactory

object RecoveryDemo extends App {

  case class Command(contents: String)

  case class Event(contents: String)

  class RecoveryActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "recovery-actor"

    override def receiveCommand: Receive = {
      case Command(contents) =>
        persist(Event(contents)) { event =>
          log.info(s"Successfully persisted $event")
        }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        log.info("I have finished recovering")
      case Event(contents) =>
        //        if (contents.contains("314"))
        //          throw new RuntimeException("I can't take this anymore!")
        //
        log.info(s"Recovered: $contents, recovery is ${if (this.recoveryFinished) "" else "NOT"} finished.")
    }

    override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.error("I failed to recover")
      super.onRecoveryFailure(cause, event)
    }

    // only if you know what you are doing
    //    override def recovery: Recovery = Recovery(toSequenceNr = 100)
    //    override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.Latest)
    //    override def recovery: Recovery = Recovery.none
  }

  val system = ActorSystem("RecoveryDemo", ConfigFactory.load().getConfig("chapter2"))
  val recoveryActor = system.actorOf(Props[RecoveryActor], "recoveryActor")
  /**
   * 1. Stashing commands
   */
  //  for (i <- 1 to 1000) {
  //    recoveryActor ! Command(s"command $i")
  //  }

  // All COMMANDS SENT DURING RECOVERY ARE STASHED

  /**
   * 2. failure during recovery
   * - onRecoveryFailure + the actor is STOPPED
   */

  /**
   * 3 - customizing recovery
   * - DO NOT persist more events after a customized incomplete recovery
   */

  /**
   * 4 - recovery status or KNOWING when you're done recovering
   * - getting a signal when you're done recovering
   */

  /**
   * 5 - stateless actors
   */
}
