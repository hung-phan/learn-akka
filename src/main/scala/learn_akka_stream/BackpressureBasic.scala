package learn_akka_stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._
import scala.language.postfixOps

object BackpressureBasic extends App {
  implicit val system = ActorSystem("BackpressureBasic")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    // simulate a long processing
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  // not backpressure
  fastSource
    .to(slowSink)
//    .run() // fusing?

  // backpressure
  fastSource.async
    .to(slowSink)
//    .run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }
  fastSource.async
    .via(simpleFlow).async
    .to(slowSink)
//    .run()

  /**
   * reactions to backpressure (in order)
   * - try to slow down if possible
   * - buffer elements until more demand
   * - drop down elements from the buffer if it is overflow
   * - tear down/kill the whole stream (failure)
   */
  val bufferFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)

  fastSource.async
    .via(bufferFlow).async
    .to(slowSink)
//    .run()

  /**
   * Overflow strategies:
   * - Drop head = Drop oldest
   * - Drop tail = Drop newest
   * - Drop new = extract element to be added = keep the buffer
   * - Drop the entire buffer
   * - Backpressure signal
   * - fail
   */

  // throttling
  fastSource.throttle(2, 1 second)
    .runWith(Sink.foreach(println))
}
