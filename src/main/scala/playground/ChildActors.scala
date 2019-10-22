package playground

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {

  object Parent {

    case class CreateChild(name: String)

    case class TellChild(message: String)

  }

  class Parent extends Actor {

    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")

        context.become(withChild(context.actorOf(Props[Child], name)))
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) =>
        childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! Parent.CreateChild("child")
  parent ! Parent.TellChild("hey Kid!")

  val childSelection = system.actorSelection("/user/parent/child")

  childSelection ! "I found you"

  object NaiveBankAccount {

    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case object InitializeAccount

    case object PrintStatement

  }

  class NaiveBankAccount extends Actor {

    import CreditCard._
    import NaiveBankAccount._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        var creditCardRef = context.actorOf(Props[CreditCard], "card")

        creditCardRef ! AttachToAccount(self)
      case Deposit(fund) => amount += fund
      case Withdraw(fund) => amount -= fund
      case PrintStatement => sender() ! amount
    }
  }

  object CreditCard {

    case class AttachToAccount(bankAccount: ActorRef)

    case object CheckStatus

  }

  class CreditCard extends Actor {

    import NaiveBankAccount._
    import CreditCard._

    override def receive: Receive = {
      case AttachToAccount(accountRef) => context.become(attachTo(accountRef))
    }

    def attachTo(accountRef: ActorRef): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed!")

        accountRef ! PrintStatement
      case number: Int =>
        println(s"${self.path} receive $number")
    }
  }

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")

  bankAccountRef ! NaiveBankAccount.InitializeAccount

  bankAccountRef ! NaiveBankAccount.Deposit(100)

  Thread.sleep(500)

  var ccSelection = system.actorSelection("/user/account/card")

  ccSelection ! CreditCard.CheckStatus
}
