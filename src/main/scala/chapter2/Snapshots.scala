package chapter2

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object Snapshots extends App {

  // commands
  case class ReceiveMessage(contents: String) // message FROM your contact
  case class SentMessage(contents: String) // message TO your contact

  // events
  case class ReceivedMessageRecord(id: Int, contents: String)

  case class SentMessageRecord(id: Int, contents: String)

  object Chat {
    def props(owner: String, contact: String) = Props(new Chat(owner, contact))
  }

  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {
    val MAX_MESSAGE = 10
    val lastMessages = new mutable.Queue[(String, String)]()
    var commandsWithoutCheckpoint = 0
    var currentMessageId = 0

    override def persistenceId: String = s"$owner-$contact-chat"

    override def receiveCommand: Receive = {
      case ReceiveMessage(contents) =>
        persist(ReceivedMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Received message: $contents")
          maybeReplaceMessage(contact, contents)
          currentMessageId += 1
          maybeCheckpoint()
        }
      case SentMessage(contents) =>
        persist(SentMessageRecord(currentMessageId, contents)) { e =>
          log.info(s"Sent message: $contents")
          maybeReplaceMessage(owner, contents)
          currentMessageId += 1
        }
      case "print" =>
        log.info(s"Most recent messages: $lastMessages")
      // snapshot-related messages
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"saving snapshot success: $metadata")
      case SaveSnapshotFailure(metadata, reason) =>
        log.warning(s"saving snapshot $metadata failed because of $reason")
    }

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, contents) =>
        log.info(s"Recovered received message $id: $contents")
        maybeReplaceMessage(owner, contents)
        currentMessageId = id
      case SentMessageRecord(id, contents) =>
        log.info(s"Recovered sent message $id: $contents")
        maybeReplaceMessage(owner, contents)
        currentMessageId = id
      case SnapshotOffer(_, contents) =>
        log.info("Recovered snapshot")
        contents.asInstanceOf[List[(String, String)]].foreach(
          lastMessages.enqueue(_)
        )
    }

    def maybeCheckpoint(): Unit = {
      commandsWithoutCheckpoint += 1

      if (commandsWithoutCheckpoint >= MAX_MESSAGE) {
        log.info("Saving checkpoint...")
        saveSnapshot(lastMessages.toList) // asynchronous operation
        commandsWithoutCheckpoint = 0
      }
    }

    def maybeReplaceMessage(sender: String, contents: String): Unit = {
      if (lastMessages.size >= MAX_MESSAGE) {
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender, contents))
    }
  }

  val system = ActorSystem("SnapshotsDemo", ConfigFactory.load().getConfig("chapter2"))
  val chat = system.actorOf(Chat.props("daniel123", "martin345"))

//  for (i <- 1 to 100000) {
//    chat ! ReceiveMessage(s"Akka Rocks $i")
//    chat ! SentMessage(s"Akka Rules $i")
//  }

  /**
   * pattern:
   * - after each persist, maybe save s snapshot (logic is up to you)
   * - if you save a snapshot, handle the SnapshotOffer message in receiveRecover
   * - (optional step, but best pratice) handle SaveSnapshotSuccess and SaveSnapshotFailure in receiveCommand
   */
}
