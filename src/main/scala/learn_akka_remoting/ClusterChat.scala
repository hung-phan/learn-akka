package learn_akka_remoting

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object ChatDomain {
  case class ChatMessage(nickname: String, contents: String)
  case class UserMessage(contents: String)
  case class EnterRoom(fullAddress: String, nickname: String)
}

class ChatActor(nickname: String, port: Int) extends Actor with ActorLogging {
  import ChatDomain._

  implicit val timeout = Timeout(2 seconds)

  val cluster = Cluster(context.system)

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

  private def getChatActor(address: String) = {
    context.actorSelection(s"$address/user/chatActor")
  }

  override def receive: Receive = online(Map())

  def online(chatRoom: Map[String, String]): Receive = {
    case MemberUp(member) =>
      val remoteActorSelection = getChatActor(member.address.toString)

      remoteActorSelection ! EnterRoom(
        s"${self.path.address}@localhost:$port",
        nickname
      )

    case MemberRemoved(member, _) =>
      chatRoom.get(member.address.toString).foreach { address =>
        val remoteNickname = chatRoom(member.address.toString)

        log.info(s"$remoteNickname left the room.")
        context.become(online(chatRoom - member.address.toString))
      }

    case EnterRoom(remoteAddress, remoteNickname) =>
      if (remoteNickname != nickname) {
        log.info(s"$remoteNickname entered the room.")
        context.become(online(chatRoom + (remoteAddress -> remoteNickname)))
      }

    case UserMessage(contents) =>
      chatRoom.keys.foreach { remoteAddressAsString =>
        getChatActor(remoteAddressAsString) ! ChatMessage(nickname, contents)
      }

    case ChatMessage(remoteNickname, contents) =>
      log.info(s"[$remoteNickname] $contents")
  }
}

class ChatApp(nickname: String, port: Int) extends App {
  import ChatDomain._

  val config = ConfigFactory
    .parseString(s"""
      |akka.remote.artery.canonical.port = $port
      |""".stripMargin)
    .withFallback(ConfigFactory.load("akka_remoting/cluster_chat.conf"))
  val system = ActorSystem("RTJVMCluster", config)
  val chatActor =
    system.actorOf(Props(new ChatActor(nickname, port)), "chatActor")

  Source.stdin.getLines().foreach { line =>
    chatActor ! UserMessage(line)
  }
}

object Alice extends ChatApp("Alice", 2551)
object Bob extends ChatApp("Bob", 2552)
object Charlie extends ChatApp("Charlie", 2553)
