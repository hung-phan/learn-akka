package learn_akka_stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import scala.concurrent.duration._
import scala.language.postfixOps

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1) // hard computation
  val multiplier = Flow[Int].map(x => x * 10) // hard computation
  val output = Sink.foreach[(Int, Int)](println)

  // create fan-out and fan-in operator
  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape
    } // graph
  ) // runnable graph

//  graph.run() // run the graph and materialize it

  /**
   * exercise 1: feed a source into 2 sinks at the same time (hint: use a broadcast)
   */
  val firstSink = Sink.foreach[Int] { e => println(s"First element: $e") }
  val secondSink = Sink.foreach[Int] { e => println(s"Second element: $e") }

  val anotherGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator

      // step 3 - tying up the components
      input ~> broadcast ~> firstSink
               broadcast ~> secondSink

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape
    } // graph
  ) // runnable graph

//  anotherGraph.run()

  /**
   * exercise 2: use merge and balance
   */
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)
  val sink1 = Sink.fold[Int, Int](0) { (count, e) =>
    println(s"Sink 1 number of elements: $count")
    count + 1
  }
  val sink2 = Sink.fold[Int, Int](0) { (count, e) =>
    println(s"Sink 2 number of elements: $count")
    count + 1
  }

  val otherGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2)) // fan-out operator
      val balance = builder.add(Balance[Int](2))

      // step 3 - tying up the components
      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge;   balance ~> sink2

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape
    } // graph
  ) // runnable graph

  otherGraph.run()
}
