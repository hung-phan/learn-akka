package learn_akka_remoting

import akka.actor.{
  Actor,
  ActorLogging,
  ActorRef,
  ActorSystem,
  Address,
  Props,
  ReceiveTimeout
}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Random, Using}

object ClusteringExampleDomain {
  case class ProcessFile(filename: String)
  case class ProcessLine(line: String, aggregator: ActorRef)
  case class ProcessLineResult(count: Int)
}

class ClusterWordCountPriorityMailbox(settings: ActorSystem.Settings,
                                      config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case _: MemberEvent => 0
      case _              => 5
    })

class Master extends Actor with ActorLogging {
  import ClusteringExampleDomain._

  implicit val timeout = Timeout(2 seconds)
  val cluster = Cluster(context.system)

  var workers: Map[Address, ActorRef] = Map()
  var pendingRemoval: Map[Address, ActorRef] = Map()

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive =
    handlerClusterEvents
      .orElse(handleWorkerRegistration)
      .orElse(handleJob)

  def handlerClusterEvents: Receive = {
    case MemberUp(member) if member.hasRole("worker") =>
      log.info(s"Member is up: ${member.address}")

      if (pendingRemoval.contains(member.address)) {
        pendingRemoval -= member.address
      } else {
        val workerSelection =
          context.actorSelection(s"${member.address}/user/worker")

        workerSelection
          .resolveOne()
          .map(ref => (member.address, ref))
          .pipeTo(self)
      }

    case UnreachableMember(member) if member.hasRole("worker") =>
      log.info(s"Member detected as unreachable: ${member.address}")

      val workerOption = workers.get(member.address)

      workerOption.foreach { ref =>
        pendingRemoval += (member.address -> ref)
      }

    case MemberRemoved(member, previousStatus) if member.hasRole("worker") =>
      log.info(s"Member ${member.address} removed after $previousStatus")

      workers -= member.address

    case m: MemberEvent =>
      log.info(s"Another member event: $m")
  }

  def handleWorkerRegistration: Receive = {
    case pair: (Address, ActorRef) =>
      log.info(s"Registering worker: $pair")
      workers += pair
  }

  def handleJob: Receive = {
    case ProcessFile(filename) =>
      val aggregator = context.actorOf(Props[Aggregator], "aggregator")

      Using(Source.fromFile(filename)) { source =>
        source
          .getLines()
          .foreach(self ! ProcessLine(_, aggregator))
      }

    case ProcessLine(line, aggregator) =>
      val availableWorkers = workers -- pendingRemoval.keys
      val workerIndex = Random.nextInt(availableWorkers.size)
      val worker = availableWorkers.values.toSeq(workerIndex)

      worker ! ProcessLine(line, aggregator)

      Thread.sleep(1000)
  }
}

class Aggregator extends Actor with ActorLogging {
  import ClusteringExampleDomain._

  context.setReceiveTimeout(3 seconds)

  override def receive: Receive = online(0)

  def online(totalCount: Int): Receive = {
    case ProcessLineResult(count) =>
      context.become(online(totalCount + count))

    case ReceiveTimeout =>
      log.info(s"TOTAL COUNT: $totalCount")
      context.setReceiveTimeout(Duration.Undefined)
  }
}

class Worker extends Actor with ActorLogging {
  import ClusteringExampleDomain._

  override def receive: Receive = {
    case ProcessLine(line, aggregator) =>
      log.info(s"Processing: $line")

      aggregator ! ProcessLineResult(line.split(" ").length)
  }
}

object ClusterDomain {
  def createNode(port: Int,
                 role: String,
                 props: Props,
                 actorName: String): ActorRef = {
    val config = ConfigFactory
      .parseString(s"""
                      |akka.cluster.roles = ["$role"]
                      |akka.remote.artery.canonical.port = $port
                      |""".stripMargin)
      .withFallback(ConfigFactory.load("akka_remoting/clustering_example.conf"))

    val system = ActorSystem("RTJVMCluster", config)

    system.actorOf(props, actorName)
  }
}

object SeedNodes extends App {
  import ClusterDomain._
  import ClusteringExampleDomain._

  val master = createNode(2551, "master", Props[Master], "master")
  createNode(2552, "worker", Props[Worker], "worker")
  createNode(2553, "worker", Props[Worker], "worker")

  Thread.sleep(10000)

  master ! ProcessFile("src/main/resources/akka_remoting/lorem_ipsum.txt")
}

object AdditionalWorker extends App {
  import ClusterDomain._

  createNode(2554, "worker", Props[Worker], "worker")
}
