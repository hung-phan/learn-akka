package chapter1

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => sender() ! "Hello there"
      case message: String => println(s"[${self}] I have received $message")
      case number: Int => println(s"[${self}] I have received a number $number")
      case SpecialMessage(content) => println(s"[${self}] I have received sth SPECIAL: $content")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "(Forwarded)")
    }
  }

  var system = ActorSystem("actorCapabilitiesDemo")

  var simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  simpleActor ! 42

  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("some special content")

  case class SendMessageToYourself(content: String)

  simpleActor ! SendMessageToYourself("I am an actor")

  // 3 - actor can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi!"

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a message with the ORIGINAL sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)

  alice ! WirelessPhoneMessage("Hi", bob)
}
