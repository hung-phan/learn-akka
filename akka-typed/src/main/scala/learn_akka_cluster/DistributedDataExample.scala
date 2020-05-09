import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory
import learn_akka_cluster.ReplicatedCache
import learn_akka_cluster.ReplicatedCache.Cached

import scala.concurrent.duration._
import scala.language.postfixOps

object DistributedDataExample extends App {

  sealed trait SystemCommand

  case object Start extends SystemCommand
  final case class GetFromCacheResponse(cached: Cached) extends SystemCommand

  def MainSystem(port: Int): Behavior[SystemCommand] =
    Behaviors.setup { ctx =>
      val replicatedCache = ctx.spawnAnonymous(ReplicatedCache())
      val getFromCacheResponse =
        ctx.messageAdapter[Cached](GetFromCacheResponse.apply)

      Behaviors.receiveMessagePartial {
        case Start =>
          if (port == 2551) {
            ctx.scheduleOnce(
              5 seconds,
              replicatedCache,
              ReplicatedCache.PutInCache("key_a", "this is a test value")
            )
          }

          ctx.scheduleOnce(
            10 seconds,
            replicatedCache,
            ReplicatedCache.GetFromCache("key_a", getFromCacheResponse)
          )

          Behaviors.same
        case GetFromCacheResponse(cached) =>
          ctx.log.info(
            s"Receive ${cached.key} with value: ${cached.value.getOrElse("")}"
          )

          Behaviors.same
      }
    }

  def startCluster(ports: List[Int]): Unit =
    ports.foreach { port =>
      val config = ConfigFactory
        .parseString(s"""
                        |akka.remote.artery.canonical.port = $port
                        |""".stripMargin)
        .withFallback(
          ConfigFactory.load("learn_akka_cluster/distributed_data_example.conf")
        )

      val system =
        ActorSystem[SystemCommand](MainSystem(port), "RTJVMCluster", config)

      system ! Start
    }

  // if you assign 0 to the port, the system will allocate a random port for you
  startCluster(List(2551, 2552, 0))
}
