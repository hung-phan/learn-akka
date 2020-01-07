package learn_akka_remoting

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

import scala.io.Source

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

  val workerRoute =
    context.actorOf(FromConfig.props(Props[WordCountWorker]), "workerRouter")

  override def receive: Receive = onlineWithRouter(0, 0)

  def onlineWithRouter(remainingTasks: Int, totalCount: Int): Receive = {
    case text: String =>
      // split it into sentences
      val sentences = text.split("\\. ")

      sentences.foreach { sentence =>
        workerRoute ! WordCountTask(sentence)
      }

      context.become(
        onlineWithRouter(remainingTasks + sentences.length, totalCount)
      )
    case WordCountResult(count) =>
      if (remainingTasks == 1) {
        log.info(s"TOTAL RESULT: ${totalCount + count}")
        context.stop(self)
      } else {
        context.become(onlineWithRouter(remainingTasks - 1, totalCount + count))
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
  val system = ActorSystem("WorkerSystem", config)
}
