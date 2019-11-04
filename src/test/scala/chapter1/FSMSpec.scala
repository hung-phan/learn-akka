package chapter1

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class FSMSpec extends TestKit(ActorSystem("FSMSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FSMSpec._

  def testVendingMachineSuite(props: Props) = {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError("MachineNotInitialized"))
    }
    "report a product not available" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))
      vendingMachine ! RequestProduct("sandwich")
      expectMsg(VendingError("ProductNotAvailable"))
    }
    "throw a timeout if I don't insert money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 1))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 1 dollars"))

      within(1.5 seconds) {
        expectMsg(VendingError("RequestTimeOut"))
      }
    }
    "handle the reception of partial money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(1)
      expectMsg(Instruction("Please insert 2 dollars"))

      within(1.5 second) {
        expectMsg(VendingError("RequestTimeOut"))
        expectMsg(GiveBackChange(1))
      }
    }
    "deliver the product if I insert all the money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(3)
      expectMsg(Deliver("coke"))
    }
    "give back change and be able to request money for a new product" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 3))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction("Please insert 3 dollars"))

      vendingMachine ! ReceiveMoney(4)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(1))
    }
  }

  "A vending machine" should {
    testVendingMachineSuite(Props[VendingMachine])
  }

  "A vending machine with FSM" should {
    testVendingMachineSuite(Props[VendingMachineFSM])
  }
}

object FSMSpec {

  /**
   * Vending Machine
   */
  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int])

  case class RequestProduct(product: String)

  case class Instruction(instruction: String) // message the VM will show on its "screen"

  case class ReceiveMoney(amount: Int)

  case class Deliver(product: String)

  case class GiveBackChange(amount: Int)

  case class VendingError(reason: String)

  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _ => sender() ! VendingError("MachineNotInitialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) =>
          sender() ! VendingError("ProductNotAvailable")
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price dollars")
          context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
      }
    }

    def startReceiveMoneyTimeoutSchedule = context.system.scheduler.scheduleOnce(1 seconds) {
      self ! ReceiveMoneyTimeout
    }

    def waitForMoney(inventory: Map[String, Int], prices: Map[String, Int], product: String, money: Int, moneyTimeoutSchedule: Cancellable, requester: ActorRef): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError("RequestTimeOut")
        if (money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))
      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()

        val price = prices(product)

        if (money + amount >= price) {
          requester ! Deliver(product)

          // deliver the change
          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)

          // updating inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)

          context.become(operational(newInventory, prices))
        } else {
          val remainingMoney = price - money - amount

          requester ! Instruction(s"Please insert $remainingMoney dollars")
          context.become(waitForMoney(inventory, prices, product, money + amount, startReceiveMoneyTimeoutSchedule, requester))
        }
    }
  }

  // step 1 - define the states and the data of the actor
  trait VendingState

  case object Idle extends VendingState

  case object Operational extends VendingState

  case object WaitForMoney extends VendingState

  trait VendingData

  case object Uninitialized extends VendingData

  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData

  case class WaitForMoney(inventory: Map[String, Int],
                          prices: Map[String, Int],
                          product: String,
                          money: Int,
                          requester: ActorRef) extends VendingData

  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    // we dont' have a receive handler

    // an Event(message, data)

    /**
     * state, data
     */
    startWith(Idle, Uninitialized)

    when(Idle) {
      case Event(Initialize(inventory, prices), Uninitialized) =>
        // equivalent with context.become(operational(inventory, prices))
        goto(Operational) using Initialized(inventory, prices)
      case _ =>
        sender() ! VendingError("MachineNotInitialized")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) =>
            sender() ! VendingError("ProductNotAvailable")
            stay()
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert $price dollars")
            goto(WaitForMoney) using WaitForMoney(inventory, prices, product, 0, sender())
        }
    }

    when(WaitForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoney(inventory, prices, product, money, requester)) =>
        requester ! VendingError("RequestTimeOut")
        if (money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory, prices)
      case Event(ReceiveMoney(amount), WaitForMoney(inventory, prices, product, money, requester)) =>
        val price = prices(product)

        if (money + amount >= price) {
          requester ! Deliver(product)

          // deliver the change
          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)

          // updating inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)

          goto(Operational) using Initialized(newInventory, prices)
        } else {
          val remainingMoney = price - money - amount

          requester ! Instruction(s"Please insert $remainingMoney dollars")

          stay() using WaitForMoney(inventory, prices, product, money + amount, requester)
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("CommandNotFound")
        stay()
    }

    onTransition {
      case stateA -> stateB => log.info(s"Transitioning from $stateA to $stateB")
    }

    initialize()
  }

}
