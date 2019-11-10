package learn_akka_basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object Exercise extends App {

  val system = ActorSystem("my_system")

  object Counter {

    case object Increment

    case object Decrement

    case object Print

  }

  class Counter extends Actor {
    var value = 0

    override def receive: Receive = {
      case Counter.Increment => value += 1
      case Counter.Decrement => value -= 1
      case Counter.Print => println(s"[${self.path}] $value")
    }
  }

  val counter = system.actorOf(Props[Counter], "counter")

  counter ! Counter.Increment
  counter ! Counter.Print
  counter ! Counter.Increment
  counter ! Counter.Print
  counter ! Counter.Decrement
  counter ! Counter.Print

  object BankAccount {

    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case class Statement()

    case class TransactionSuccess()

    case class TransactionFailure(reason: String)

    def props(amount: Int) = Props(new BankAccount(amount))
  }

  class BankAccount(amount: Int) extends Actor {
    var state = amount

    override def receive: Receive = {
      case BankAccount.Deposit(value) => {
        if (value < 0) {
          sender() ! BankAccount.TransactionFailure("invalid value")
        } else {
          state += value

          sender() ! BankAccount.TransactionSuccess()
        }
      }
      case BankAccount.Withdraw(value) => {
        if (value < 0) {
          sender() ! BankAccount.TransactionFailure("invalid value")
        } else {
          if (value > state) {
            sender() ! BankAccount.TransactionFailure("insufficient fund")
          } else {
            state -= value

            sender() ! BankAccount.TransactionSuccess()
          }
        }
      }
      case BankAccount.Statement() => sender() ! s"Your balance is $state"
    }
  }

  case class LiveTheLife(account: ActorRef)

  class Person extends Actor {
    override def receive: Receive = {
      case LiveTheLife(ref) => {
        ref ! BankAccount.Deposit(10000)
        ref ! BankAccount.Withdraw(90000)
        ref ! BankAccount.Withdraw(500)
        ref ! BankAccount.Statement()
      }
      case message => println(message.toString)
    }
  }

  val bankAccount = system.actorOf(BankAccount.props(0), "bankAccount")
  var person = system.actorOf(Props[Person], "billionare")

  person ! LiveTheLife(bankAccount)
}
