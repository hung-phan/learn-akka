package learn_akka_stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MaterializingStreams extends App {
  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  val sumFuture = source.runWith(sink)

  sumFuture.onComplete {
    case Success(value) => println(s"The sum of all elements is: $value")
    case Failure(exception) => println(s"The sum of the elements could not be computed: $exception")
  }

  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach[Int](println)

//  simpleSource.viaMat(simpleFlow)((sourceMat, flowMat) => flowMat)
    val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)

  graph.run().onComplete {
    case Success(_) => println("Stream processing finished.")
    case Failure(exception) => println(s"Stream processing failed with: $exception")
  }

  // sugars
  val sum = Source(1 to 10).runWith(Sink.reduce[Int](_ + _))
  Source(1 to 10).runReduce(_ + _)

  // backwards
  Sink.foreach[Int](println).runWith(Source.single(42))

  // both ways
  Flow[Int].map(x => 2 * x).runWith(simpleSource, simpleSink)

  /**
   * - return the last element out of a source (use Sink.last)
   * - compute the total word count of a stream of sentences
   *   - map, fold, reduce
   */
  val res1 = Source(1 to 10).toMat(Sink.last[Int])(Keep.right)
  res1.run().onComplete {
    case Success(value) => println(s"The last value is $value")
    case Failure(exception) => println(s"Get exception $exception")
  }

  val res2 = Source("a b c a b b c a a a c d".split(" "))
    .fold[mutable.Map[String, Int]](mutable.Map())((currentMap, string) => {
      currentMap.put(string, currentMap.getOrElse(string, 0) + 1)
      currentMap
    })
    .runWith(Sink.foreach(println))

  // more example
  val f1 = Source(1 to 10).runWith(Sink.last)
}
