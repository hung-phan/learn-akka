package learn_akka_cluster

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps
import scala.util.Random

case class Person(id: String, age: Int)

object Person {
  def generate() = Person(UUID.randomUUID().toString, 16 + Random.nextInt(90))
}

sealed trait Command
case object Start extends Command
case class Vote(person: Person, candidate: String, replyTo: ActorRef[Command])
    extends Command
case object ReceiveTimeout extends Command
case object VoteAccepted extends Command
case class VoteRejected(reason: String) extends Command

object VotingAggregator {
  val CANDIDATES: Set[String] = Set("Martin", "Roland", "Jonas", "Daniel")

  implicit val timeout = Timeout(2 seconds)

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      ctx.setReceiveTimeout(2 minutes, ReceiveTimeout)

      def online(personsVoted: Set[String],
                 polls: Map[String, Int]): Behavior[Command] =
        Behaviors.receiveMessagePartial {
          case Vote(Person(id, age), candidate, replyTo) =>
            if (personsVoted.contains(id)) {
              replyTo ! VoteRejected("Already voted")
              Behaviors.same
            } else if (age < 18) {
              replyTo ! VoteRejected("Not above legal voting age")
              Behaviors.same
            } else if (!CANDIDATES.contains(candidate)) {
              replyTo ! VoteRejected("Invalid candidate")
              Behaviors.same
            } else {
              ctx.log.info(s"Recording vote from person $id for $candidate")
              replyTo ! VoteAccepted

              online(
                personsVoted + id,
                polls + (candidate -> (polls.getOrElse(candidate, 0) + 1))
              )
            }
          case ReceiveTimeout =>
            ctx.log.info(s"TIME'S UP, here are the poll result: ${polls}")
            ctx.cancelReceiveTimeout()
            offline()
        }

      def offline(): Behavior[Command] =
        Behaviors.receiveMessagePartial {
          case v: Vote =>
            ctx.log.warn(s"Received $v, which is invalid as the time is up")
            v.replyTo ! VoteRejected(
              "Cannot accept votes after the polls closing time"
            )
            Behaviors.same

          case m =>
            ctx.log.warn(
              s"Received $m - will not process more messages after polls closing time"
            )
            Behaviors.same
        }

      online(Set(), Map())
    }
}

object CentralElectionSystem {
  def apply(appIdentifier: String): Behavior[Command] =
    Behaviors.setup { ctx =>
      val singletonManager = ClusterSingleton(ctx.system)
      val votingAggregator: ActorRef[Command] = singletonManager.init(
        SingletonActor(
          Behaviors
            .supervise(VotingAggregator())
            .onFailure[Exception](SupervisorStrategy.restart),
          "votingAggregator"
        )
      )

      Behaviors.receiveMessagePartial {
        case Start =>
          Future {
            Source.stdin.getLines().foreach { candidate =>
              votingAggregator ! Vote(Person.generate(), candidate, ctx.self)
            }
          }

          Behaviors.same

        case VoteAccepted =>
          ctx.log.info("Vote was accepted")
          Behaviors.same

        case VoteRejected(reason) =>
          ctx.log.warn(s"Vote was rejected: $reason")
          Behaviors.same
      }
    }
}

class VotingStationApp(port: Int) extends App {
  val config = ConfigFactory
    .parseString(s"""
                    |akka.remote.artery.canonical.port = $port
                    |""".stripMargin)
    .withFallback(
      ConfigFactory.load("learn_akka_cluster/voting_system_singleton.conf")
    )
  val system =
    ActorSystem[Command](
      CentralElectionSystem(s"app_$port"),
      "RTJVMCluster",
      config
    )

  // TODO 1: set up the cluster singleton here
  system ! Start
}

object Washington extends VotingStationApp(2551)
object NewYork extends VotingStationApp(2552)
object SanFrancisco extends VotingStationApp(2553)
