package learn_akka_cluster

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.cluster.ClusterEvent.{
  MemberEvent,
  MemberJoined,
  MemberRemoved,
  MemberUp
}
import akka.cluster.typed.{Cluster, Subscribe, Unsubscribe}
import com.typesafe.config.ConfigFactory

object ClusterBasics extends App {
  sealed trait Command
  case object Start extends Command
  case object Stop extends Command

  object MemberEventListener {
    def apply(cluster: Cluster): Behavior[MemberEvent] =
      Behaviors.setup { ctx =>
        cluster.subscriptions ! Subscribe(ctx.self, classOf[MemberEvent])

        Behaviors
          .receiveMessagePartial[MemberEvent] {
            case MemberJoined(member) =>
              ctx.log.info(s"New member in town: ${member.address}")
              Behaviors.same
            case MemberUp(member) if member.hasRole("numberCruncher") =>
              ctx.log.info(s"HELLO BROTHER: ${member.address}")
              Behaviors.same
            case MemberUp(member) =>
              ctx.log.info(
                s"Let's say welcome to the newest member: ${member.address}"
              )
              Behaviors.same
            case MemberRemoved(member, previousStatus) =>
              ctx.log.info(
                s"Poor ${member.address}, it was removed from $previousStatus"
              )
              Behaviors.same
            case m: MemberEvent =>
              ctx.log.info(s"Another member event: $m")
              Behaviors.same
          }
          .receiveSignal {
            case (ctx, PostStop) =>
              cluster.subscriptions ! Unsubscribe(ctx.self)
              Behaviors.ignore
          }
      }
  }

  object MainSystem {
    def apply(): Behavior[Command] =
      Behaviors.setup { ctx =>
        ctx.spawnAnonymous(MemberEventListener(Cluster(ctx.system)))

        Behaviors.receivePartial {
          case (ctx, Start) =>
            ctx.log.info(s"Cluster start with ${ctx.self.path}")
            Behaviors.same
          case (ctx, Stop) =>
            Behaviors.stopped
        }
      }
  }

  def startCluster(ports: List[Int]): Unit =
    ports.foreach { port =>
      val config = ConfigFactory
        .parseString(s"""
             |akka.remote.artery.canonical.port = $port
             |""".stripMargin)
        .withFallback(
          ConfigFactory.load("learn_akka_cluster/cluster_basics.conf")
        )

      val system =
        ActorSystem[Command](MainSystem(), "RTJVMCluster", config)

      system ! Start
    }

  // if you assign 0 to the port, the system will allocate a random port for you
  startCluster(List(2551, 2552, 0))
}
