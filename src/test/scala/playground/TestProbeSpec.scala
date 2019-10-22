package playground

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  import TestProbeSpec._

  override protected def afterAll(): Unit = {
    super.afterAll()

    TestKit.shutdownActorSystem(system)
  }

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)

      expectMsg(RegistrationAck)
    }

    "send the work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)

      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"

      master ! Work(workloadString)

      slave.expectMsg(SlaveWork(workloadString, testActor))
      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)

      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"

      master ! Work(workloadString)
      master ! Work(workloadString)

      slave.receiveWhile() {
        case SlaveWork(`workloadString`, `testActor`) =>
          slave.reply(WorkCompleted(3, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec {

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) => {
        context.become(online(slaveRef, 0))
        sender() ! RegistrationAck
      }
      case _ => //ignore
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count

        originalRequester ! Report(newTotalWordCount)

        context.become(online(slaveRef, newTotalWordCount))
    }
  }

  case class Work(text: String)

  case class SlaveWork(text: String, originalRequester: ActorRef)

  case class WorkCompleted(count: Int, originalRequester: ActorRef)

  case class Register(slaveRef: ActorRef)

  case object RegistrationAck

  case class Report(totalCount: Int)

}
