package chapter1

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration
import scala.concurrent.duration.Duration
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  import BasicSpec._

  override protected def afterAll(): Unit = {
    super.afterAll()

    TestKit.shutdownActorSystem(system)
  }

  "A simple actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"

      echoActor ! message

      expectMsg(message)
    }
  }

  "A blackhole actor" should {
    "send back some messages" in {
      val blackholeActor = system.actorOf(Props[BlackholeActor])
      val message = "hello, test"

      blackholeActor ! message

      expectNoMessage(Duration(1, duration.SECONDS))
    }
  }

  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I love akka"

      val reply = expectMsgType[String]

      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"

      expectMsgAnyOf[String]("hi", "hello")
    }

    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"

      expectMsgAllOf[String]("Scala", "Akka")
    }

    "reply with cool tech in a different way" in {
      labTestActor ! "favoriteTech"

      val messages = receiveN(2)
      // free to do more complicated assertions
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favoriteTech"

      expectMsgPF() {
        case "Scala" => //only care that the pf is defined
          assert(true)
        case "Akka" =>
          assert(true)
        case _ =>
          assert(false)
      }
    }
  }
}

object BasicSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackholeActor extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()

    override def receive: Receive = {
      case "greeting" => sender() ! (if (random.nextBoolean()) "hi" else "hello")
      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase()
    }
  }

}
