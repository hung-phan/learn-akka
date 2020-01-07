package learn_akka_stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {
  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /**
   * A composite component (sink)
   * - prints out all strings which are lowercase
   * - COUNTS the string that are shots (< 5 chars)
   */
  // step 1
  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter)((_, counterMatVal) => counterMatVal) {
      implicit builder =>
        (printerShape, counterShape) =>
          import GraphDSL.Implicits._

          // step 2
          val broadcast = builder.add(Broadcast[String](2))
          val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
          val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

          broadcast ~> lowercaseFilter ~> printerShape
          broadcast ~> shortStringFilter ~> counterShape

          SinkShape(broadcast.in)
    }
  )

  import system.dispatcher

  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
  shortStringCountFuture.onComplete {
    case Success(value) => println(s"The total number of short strings is: $value")
    case Failure(exception) => println(s"The count of short strings failed: $exception")
  }

  /**
   * Exercise
   */
  /**
   * Hint: use a broadcast and a Sink.fold
   */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counter = Sink.fold[Int, B](0)((counter, _) => counter + 1)

    Flow.fromGraph(
      GraphDSL.create(counter) { implicit builder =>
        counterShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShape = builder.add(flow)

          originalFlowShape ~> broadcast ~> counterShape

          FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()

  enhancedFlowCountFuture.onComplete {
    case Success(count) => println(s"$count elements went through the enhanced flow")
    case _ => println("Sth failed")
  }
}
