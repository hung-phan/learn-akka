package learn_akka_stream

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // this runs on the SAME ACTOR
//  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()
  // operator/component FUSION

  // equivalent behavior
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case x: Int =>
        // flow operations
        val x2 = x + 1
        val y = x2 * 10
        // sink operation
        println(y)
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor])
//  (1 to 1000).foreach(simpleActor ! _)

  // complex flows:
  val complexFlow = Flow[Int].map { x =>
    Thread.sleep(1000)
    x + 1
  }
  val complexFlow2 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x * 10
  }

//  simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()
  // async boundary. this will force running the flow on different actor and
  // speed up processing
//  simpleSource
//    .via(complexFlow).async
//    .via(complexFlow2).async
//    .to(simpleSink)
//    .run()

  // ordering guarantees
  Source(1 to 3)
    .map(elem => { println(s"Flow A: $elem"); elem }).async
    .map(elem => { println(s"Flow B: $elem"); elem }).async
    .map(elem => { println(s"Flow C: $elem"); elem }).async
    .runWith(Sink.ignore)
}
