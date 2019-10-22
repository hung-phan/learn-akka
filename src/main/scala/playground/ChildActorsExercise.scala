package playground

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.BalancingPool

object ChildActorsExercise extends App {

  // Distributed word count

  object WordCounterMaster {

    case class Initialize(nChildren: Int)

    case class WordCountTask(text: String)

    case class WordCountReply(count: Int)

  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    var router: ActorRef = null
    var count: Int = 0

    override def receive: Receive = {
      case Initialize(nChildren) =>
        router = context.actorOf(BalancingPool(nChildren).props(Props[WordCounterWorker]), "router")
      case WordCountTask(text) =>
        router ! WordCountTask(text)
      case WordCountReply(res) => {
        count += res
        println(s"Current value: $count")
      }
    }
  }

  class WordCounterWorker extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(text) =>
        sender() ! WordCountReply(text.split(" ").length)
    }
  }

  val system = ActorSystem("ChildActorsExercise")

  var master = system.actorOf(Props[WordCounterMaster])

  master ! WordCounterMaster.Initialize(10)
  for { i <- 0 until 100} {
    master ! WordCounterMaster.WordCountTask("how are you doing?")
  }
}
