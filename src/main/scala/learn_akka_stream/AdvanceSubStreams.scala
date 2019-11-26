package learn_akka_stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object AdvanceSubStreams extends App {
  implicit val system = ActorSystem()
  implicit val materialized = ActorMaterializer()

  // 1 - grouping a stream by a certain function
  val wordsSource = Source(List("Akka", "is", "amazing", "learning", "substreams"))
  val groups = wordsSource.groupBy(30, word =>
    if (word.isEmpty) "" else word.toLowerCase.charAt(0)
  )

  groups
    .to(Sink.fold(0)((count, word) => {
      val newCount = count + 1
      println(s"I just received $word, count is $newCount")
      newCount
    }))
    .run()

  // 2 - merge substreams back
  val textSource = Source(List(
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM"
  ))

  val totalCharCountFuture = textSource
    .groupBy(2, string => string.length % 2)
    .map(_.length) // do your expensive computation here
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  totalCharCountFuture.onComplete {
    case Success(value) => println(s"Total char count: $value")
    case Failure(exception) => println(s"Char computation failed: $exception")
  }

  // 3 - splitting a stream into substreams, when a condition is met
  val text =
    """
      |I love Akka Streams
      |this is amazing
      |learning from Rock the JVM
      |""".stripMargin

  val anotherCharCountFuture = Source(text.toList)
    .splitWhen(c => c == '\n')
    .filter(_ != '\n')
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  anotherCharCountFuture.onComplete {
    case Success(value) => println(s"Total char count alternative: $value")
    case Failure(exception) => println(s"Char computation failed alternative: $exception")
  }

  // 4 - flattening
  val simpleSource = Source(1 to 5)

  simpleSource.flatMapConcat(x => Source(x to (3 * x))).runWith(Sink.foreach(println))
  // the number of parallel streams to be merged
  simpleSource.flatMapMerge(2, x => Source(x to (3 * x))).runWith(Sink.foreach(println))
}
