package learn_akka_cluster

import java.util.{Date}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding.Passivate
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object ClusterShardingExample {
  sealed trait Command

  case class OysterCard(id: String, amount: Double) extends Command
  case class EntryAttempt(oysterCard: OysterCard,
                          date: Date,
                          replyTo: ActorRef[Command])
      extends Command
  case class EntryRejected(reason: String) extends Command
  case object EntryAccepted extends Command
  case object Start extends Command
  case object TerminateValidator extends Command
  case object ReceiveTimeout extends Command

  object Turnstile {
    def apply(
      validator: ActorRef[ShardingEnvelope[Command]]
    ): Behavior[Command] =
      Behaviors.setup { ctx =>
        Behaviors.receiveMessagePartial {
          case o: OysterCard =>
            validator ! ShardingEnvelope(o.id, EntryAttempt(o, new Date, ctx.self))
            Behaviors.same

          case EntryAccepted =>
            ctx.log.info("GREEN: please pass")
            Behaviors.same

          case EntryRejected(reason) =>
            ctx.log.info(s"RED: $reason")
            Behaviors.same
        }
      }
  }

  object OysterCardValidator {
    def apply(
      entity: String,
      shard: ActorRef[ClusterSharding.ShardCommand]
    ): Behavior[Command] =
      Behaviors.setup { ctx =>
        ctx.log.info("Validator starting")
        ctx.setReceiveTimeout(10 seconds, ReceiveTimeout)

        Behaviors.receiveMessagePartial {
          case EntryAttempt(card @ OysterCard(id, amount), _, replyTo) =>
            ctx.log.info(s"Validating $card")

            if (amount > 2.5) replyTo ! EntryAccepted
            else
              replyTo ! EntryRejected(
                s"[$id] Insufficient funds, please top up"
              )

            Behaviors.same

          case ReceiveTimeout =>
            shard ! Passivate(ctx.self)
            Behaviors.same

          case TerminateValidator => // T am sure that I won't be contacted again, so safe to stop
            Behaviors.stopped
        }
      }
  }

  object MainSystem {
    val TypeKey = EntityTypeKey[Command]("OysterCardValidator")

    def apply(numberOfTurnstiles: Int): Behavior[Command] =
      Behaviors.setup { ctx =>
        ctx.log.info("Start sharding system")

        val sharding = ClusterSharding(ctx.system)
        val shardRegion: ActorRef[ShardingEnvelope[Command]] =
          sharding.init(
            Entity(TypeKey)(
              entityContext =>
                OysterCardValidator(entityContext.entityId, entityContext.shard)
            ).withStopMessage(TerminateValidator)
          )
        val turnstiles: IndexedSeq[ActorRef[Command]] =
          (1 to numberOfTurnstiles)
            .map(id => ctx.spawn(Turnstile(shardRegion), s"turnstile_${id}"))

        Behaviors.receiveMessagePartial {
          case Start =>
            Future {
              Thread.sleep(10000)

              for (_ <- 1 to 5000) {
                val randomTurnstileIndex = Random.nextInt(numberOfTurnstiles)
                val randomTurnstile = turnstiles(randomTurnstileIndex)

                randomTurnstile ! OysterCard(
                  s"${Random.nextInt(10)}-Oystercard",
                  Random.nextDouble() * 10
                )

                Thread.sleep(50)
              }
            }

            Behaviors.same
        }
      }
  }
}

/**
  * There must be NO two messages M1 and M2 for which
  * extractEntityId(M1) == extractEntityId(M2) and extractShardId(M1) != extractShardId(M2)
  *
  * M1 -> E37, S9
  * M2 -> E37, S10
  *
  * OTHERWISE, if you use actor persistence, it can corrupt journal
  */
class TubeStation(port: Int, numberOfTurnstiles: Int) extends App {
  val config = ConfigFactory
    .parseString(s"""
                      |akka.remote.artery.canonical.port = $port
                      |""".stripMargin)
    .withFallback(
      ConfigFactory.load("learn_akka_cluster/cluster_sharding_example.conf")
    )
  val system =
    ActorSystem(
      ClusterShardingExample.MainSystem(numberOfTurnstiles),
      "RTJVMCluster",
      config
    )

  system ! ClusterShardingExample.Start
}

object PiccadillyCircus extends TubeStation(2551, 10)
object Westminster extends TubeStation(2552, 5)
object CharingCross extends TubeStation(2553, 15)
