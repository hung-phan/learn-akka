package learn_akka_basic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable

object Exercise2 extends App {

  val system = ActorSystem("my_system")

  object Counter {

    case object Increment

    case object Decrement

    case object Print

  }

  class Counter extends Actor {

    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment => context.become(countReceive(currentCount + 1))
      case Decrement => context.become(countReceive(currentCount - 1))
      case Print => println(s"[counter] my current count is $currentCount")
    }
  }

  val counter = system.actorOf(Props[Counter], "counter")

  counter ! Counter.Increment
  counter ! Counter.Print
  counter ! Counter.Increment
  counter ! Counter.Print
  counter ! Counter.Decrement
  counter ! Counter.Print

  case class Vote(candidate: String)

  case object VoteStatusRequest

  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    override def receive: Receive = voteReceive(None)

    def voteReceive(candidate: Option[String]): Receive = {
      case Vote(candidate) => context.become(voteReceive(Some(candidate)))
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor {
    val result: mutable.Map[String, Int] = mutable.Map()

    override def receive: Receive = {
      case AggregateVotes(citizens) => citizens.foreach(_ ! VoteStatusRequest)
      case VoteStatusReply(candidate) => {
        candidate.foreach(possible_candidate => result.updateWith(possible_candidate) {
          case Some(count) => Some(count + 1)
          case None => Some(1)
        })
        println(result)
      }
    }
  }

  var alice = system.actorOf(Props[Citizen])
  var bob = system.actorOf(Props[Citizen])
  var charlie = system.actorOf(Props[Citizen])
  var daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  var voteAggregator = system.actorOf(Props[VoteAggregator])

  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))
}
