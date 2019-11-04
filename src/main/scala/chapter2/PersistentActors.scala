package chapter2

import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory

object PersistentActors extends App {

  /**
   * Scenario: we have a business and an accountant which keeps track of our invoices.
   */

  // COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)

  case class InvoiceBulk(invoices: List[Invoice])

  // Special messages
  case object Shutdown

  // EVENTS
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {
    var latestInvoiceId = 0
    var totalAmount = 0

    override def persistenceId: String = "simple-accountant" // best practice: make it unique

    /**
     * The "normal" receive method
     */
    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>

        /**
         * When you receive a command
         * 1) you create an EVENT to persist into the store
         * 2) you persist the event, the pass in a callback that will get triggered once the event is written
         * 3) we update the actor's state when the event has persisted
         */
        log.info(s"Receive invoice for amount: $amount")

        persist(InvoiceRecorded(latestInvoiceId, recipient, date, amount)) { e =>
          // SAFE to access mutable state here
          // Akka persistence guarantee this
          // time gap: all other messages sent to this actor are STASHED
          //update state
          latestInvoiceId += 1
          totalAmount += amount

          // correctly identify the sender of the COMMAND
          sender() ! "PersistenceACK"

          log.info(s"Persisted $e as invoice #${e.id} for total amount $totalAmount")
        }
      case InvoiceBulk(invoices) =>

        /**
         * 1) create events (plural)
         * 2) persist all the events
         * 3) update the actor state when each event is persisted
         */
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map { pair =>
          val invoice = pair._1
          val id = pair._2

          InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }

        persistAll(events) { e =>
          latestInvoiceId += 1
          totalAmount += e.amount
          log.info(s"Persisted SINGLE $e as invoice #${e.id} for total amount $totalAmount")
        }
      case Shutdown =>
        context.stop(self)

      case "print" =>
        log.info(s"Latest invoice id: $latestInvoiceId, total amount: $totalAmount")
    }

    /**
     * Handler that will be called on recovery
     */
    override def receiveRecover: Receive = {
      /**
       * Best practice: follow the logic in the persist steps of receiveCommand
       */
      case InvoiceRecorded(id, _, _, amount) =>
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
        latestInvoiceId = id
        totalAmount += amount
    }

    /**
     * Persistence failures
     */

    /**
     * This method is called if persisting failed.
     * The actor will be STOPPED.
     *
     * Best practice: start the actor again after a while.
     * (use Backoff supervisor)
     */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      super.onPersistFailure(cause, event, seqNr)
      log.error(s"Fail to persist $event because of $cause")
    }

    /**
     * Called if the JOURNAL fails to persist the event
     * The actor is RESUMED.
     */
    override protected def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      super.onPersistRejected(cause, event, seqNr)
      log.error(s"Persist rejected $event because of $cause")
    }
  }

  val system = ActorSystem("PersistentActors", ConfigFactory.load().getConfig("chapter2"))
  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

  // Uncomment this for initial data
  //  for (i <- 1 to 10) {
  //    accountant ! Invoice("The Sofa Company", new Date(), i * 1000)
  //  }

  /**
   * Persisting multiple events
   * - persistAll
   */
  // Uncomment this for initial data
  //  val newInvoices = for (i <- 1 to 5)
  //    yield Invoice("The awesome chairs", new Date(), i * 2000)
  //  accountant ! InvoiceBulk(newInvoices.toList)

  /**
   * NEVER EVER CALL PERSIST OR PERSIST_ALL FROM FUTURES.
   */

  /**
   * Shutdown of persistent actors
   *
   * Best practice: define your own "shutdown" message
   */
}
