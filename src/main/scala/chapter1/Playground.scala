package chapter1

import akka.actor.{Actor, ActorSystem, Props}

object Playground extends App {
  val actorSystem = ActorSystem("HelloAkka")

  println(actorSystem.name)

  class WordCounterActor extends Actor {
    var totalWords = 0

    override def receive: Receive = {
      case message: String => {
        println(s"Receive: $message")
        totalWords += message.split(" ").length
      }
      case message => println(s"I don't understand $message")
    }
  }

  val wordCounter1 = actorSystem.actorOf(Props[WordCounterActor], "wordCounter1")
  val wordCounter2 = actorSystem.actorOf(Props[WordCounterActor], "wordCounter2")

  wordCounter1 ! "How are you doing?"
  wordCounter2 ! "Testing"

  object Person {
    def props(name: String) = Props(new Person(name))
  }

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
    }
  }

  val person = actorSystem.actorOf(Person.props("Hung"))

  person ! "hi"
}
