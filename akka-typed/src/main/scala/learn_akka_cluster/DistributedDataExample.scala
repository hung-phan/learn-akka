import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
import akka.cluster.ddata.{GCounter, GCounterKey, SelfUniqueAddress}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object DistributedDataExample extends App {
  object Counter {
    sealed trait Command

    case object Start extends Command
    case object Increment extends Command
    final case class GetValue(replyTo: ActorRef[Int]) extends Command
    final case class GetCachedValue(replyTo: ActorRef[Int]) extends Command
    case object Unsubscribe extends Command

    private sealed trait InternalCommand extends Command

    private case class InternalUpdateResponse(
      rsp: Replicator.UpdateResponse[GCounter]
    ) extends InternalCommand
    private case class InternalGetResponse(
      rsp: Replicator.GetResponse[GCounter],
      replyTo: ActorRef[Int]
    ) extends InternalCommand
    private case class InternalSubscribeResponse(
      chg: Replicator.SubscribeResponse[GCounter]
    ) extends InternalCommand

    def apply(key: GCounterKey): Behavior[Command] =
      Behaviors.setup[Command] { ctx =>
        implicit val node: SelfUniqueAddress =
          DistributedData(ctx.system).selfUniqueAddress

        // adapter that turns the response messages from the replicator into our own protocol
        DistributedData.withReplicatorMessageAdapter[Command, GCounter] {
          replicatorAdapter =>
            // Subscribe to changes of the given key.
            replicatorAdapter.subscribe(key, InternalSubscribeResponse.apply)

            def updated(cachedValue: Int): Behavior[Command] = {
              Behaviors.receiveMessage[Command] {
                case Increment =>
                  replicatorAdapter.askUpdate(
                    askReplyTo =>
                      Replicator.Update(
                        key,
                        GCounter.empty,
                        Replicator.WriteLocal,
                        askReplyTo
                      )(_ :+ 1),
                    InternalUpdateResponse.apply
                  )

                  Behaviors.same

                case GetValue(replyTo) =>
                  replicatorAdapter.askGet(
                    askReplyTo =>
                      Replicator.Get(key, Replicator.ReadLocal, askReplyTo),
                    value => InternalGetResponse(value, replyTo)
                  )

                  Behaviors.same

                case GetCachedValue(replyTo) =>
                  replyTo ! cachedValue
                  Behaviors.same

                case Unsubscribe =>
                  replicatorAdapter.unsubscribe(key)
                  Behaviors.same

                case internal: InternalCommand =>
                  internal match {
                    case InternalUpdateResponse(_) => Behaviors.same // ok

                    case InternalGetResponse(
                        rsp @ Replicator.GetSuccess(key),
                        replyTo
                        ) =>
                      val value = rsp.get(key).value.toInt
                      replyTo ! value
                      Behaviors.same

                    case InternalGetResponse(res, _) =>
                      ctx.log.error(res.toString)

                      Behaviors.same // not dealing with failures
                    case InternalSubscribeResponse(
                        chg @ Replicator.Changed(key)
                        ) =>
                      val value = chg.get(key).value.intValue
                      updated(value)

                    case InternalSubscribeResponse(Replicator.Deleted(_)) =>
                      Behaviors.unhandled // no deletes
                  }
              }
            }

            updated(cachedValue = 0)
        }
      }
  }

  import Counter._

  def ReportCounterValActor(): Behavior[Int] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage { msg =>
        ctx.log.warn(msg.toString)
        Behaviors.same
      }
    }

  def MainSystem(): Behavior[Command] =
    Behaviors.setup { ctx =>
      val counterActor =
        ctx.spawn(Counter(GCounterKey("simpleCounter")), "simpleCounterActor")
      val intActor = ctx.spawnAnonymous(ReportCounterValActor())

      Behaviors.receiveMessagePartial {
        case Start =>
          ctx.scheduleOnce(5 seconds, counterActor, GetValue(intActor))

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
        ActorSystem[Command](MainSystem(), "RTJVMCluster", config)

      system ! Start
    }

  // if you assign 0 to the port, the system will allocate a random port for you
  startCluster(List(2551, 2552, 0))
}
