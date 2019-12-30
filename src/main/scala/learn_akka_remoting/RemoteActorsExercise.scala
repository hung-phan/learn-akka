package learn_akka_remoting

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, ActorSystem, Identify, PoisonPill, Props, Stash}
import com.typesafe.config.ConfigFactory

import scala.io.Source
import scala.math.{max, min}

object WordCountDomain {
  case class Initialize(nWorkers: Int)
  case class WordCountTask(text: String)
  case class WordCountResult(count: Int)
  case object EndWordCount
}

class WordCountWorker extends Actor with ActorLogging {
  import WordCountDomain._

  override def receive: Receive = {
    case WordCountTask(text) =>
      log.info(s"I'm processing: $text")
      sender() ! WordCountResult(text.split(" ").length)
  }
}

class WordCountMaster extends Actor with ActorLogging with Stash {
  import WordCountDomain._

  override def receive: Receive = initialize(List(), 0)

  def initialize(workers: List[ActorRef], nWorkers: Int): Receive = {
    case Initialize(nWorkers) =>
      (1 to min(5, max(1, nWorkers))).foreach { i =>
        val selection = context.actorSelection(
          s"akka://WorkersSystem@localhost:2552/user/workCountWorker${i}"
        )

        selection ! Identify(i)
      }

      context.become(initialize(workers, nWorkers))
    case ActorIdentity(_, Some(actorRef)) =>
      val newWorkers = actorRef :: workers

      if (workers.length == nWorkers - 1) {
        unstashAll()
        context.become(online(newWorkers, 0, 0))
      } else {
        context.become(initialize(newWorkers, nWorkers))
      }
    case _ =>
      stash()
  }

  def online(workers: List[ActorRef],
             remainingTasks: Int,
             totalCount: Int): Receive = {
    case text: String =>
      // split it into sentences
      val sentences = text.split("\\. ")

      Iterator
        .continually(workers)
        .flatten
        .zip(sentences.iterator)
        .foreach { pair =>
          // send sentences to workers in turn
          val (worker, sentence) = pair

          log.info(s"Send task to $worker")

          worker ! WordCountTask(sentence)
        }
      context.become(
        online(workers, remainingTasks + sentences.length, totalCount)
      )
    case WordCountResult(count) =>
      if (remainingTasks == 1) {
        log.info(s"TOTAL RESULT: ${totalCount + count}")

        workers.foreach(_ ! PoisonPill)
        context.stop(self)
      } else {
        context.become(online(workers, remainingTasks - 1, totalCount + count))
      }
  }
}

object MasterApp extends App {
  import WordCountDomain._

  val config = ConfigFactory
    .parseString("""
      |akka.remote.artery.canonical.port = 2551
      |""".stripMargin)
    .withFallback(
      ConfigFactory.load("akka_remoting/remote_actors_exercise.conf")
    )
  val system = ActorSystem("MasterSystem", config)

  val master = system.actorOf(Props[WordCountMaster], "wordCountMaster")

  master ! Initialize(5)

  val source =
    Source.fromFile("src/main/resources/akka_remoting/lorem_ipsum.txt")

  source.getLines().foreach { line =>
    master ! line
  }

  source.close()
}

object WorkersApp extends App {

  val config = ConfigFactory
    .parseString("""
                   |akka.remote.artery.canonical.port = 2552
                   |""".stripMargin)
    .withFallback(
      ConfigFactory.load("akka_remoting/remote_actors_exercise.conf")
    )
  val system = ActorSystem("WorkersSystem", config)

  (1 to 5).map(
    i => system.actorOf(Props[WordCountWorker], s"workCountWorker$i")
  )
}
