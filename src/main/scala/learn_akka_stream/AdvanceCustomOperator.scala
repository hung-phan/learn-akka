package learn_akka_stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, Attributes, FlowShape, Inlet, Outlet, SinkShape, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object AdvanceCustomOperator extends App {
  implicit val system = ActorSystem("CustomOperators")
  implicit val materializer = ActorMaterializer()

  // 1 - a custom source which emits random numbers until canceled
  class RandomNumberGenerator(max: Int) extends GraphStage[SourceShape[Int]] {
    // step 1: define the ports and the component-specific members
    val outPort = Outlet[Int]("randomGenerator")
    val random = new Random()

    // step 3: create the logic
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        // step 4: define mutable state

        setHandler(outPort, new OutHandler {
          // when there is demand from downstream
          override def onPull(): Unit = {
            // emit a new element
            val nextNumber = random.nextInt(max)

            // push it out of the outPort
            push(outPort, nextNumber)
          }
        })
      }

    // step 2: construct a new shape
    override def shape: SourceShape[Int] = SourceShape(outPort)
  }

  val randomNumberGenerator = Source.fromGraph(new RandomNumberGenerator(100))

  randomNumberGenerator
//    .runWith(Sink.foreach(println))

  // 2 - a custom sink that prints elements in batches of a given size
  class Batcher(batchSize: Int) extends GraphStage[SinkShape[Int]] {
    val inPort = Inlet[Int]("batcher")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        // mutable state
        val batch = new mutable.Queue[Int]()

        override def preStart(): Unit = {
          pull(inPort)
        }

        setHandler(inPort, new InHandler {
          // when the upstream wants to send me an element
          override def onPush(): Unit = {
            val nextElement = grab(inPort)

            batch.enqueue(nextElement)

            // assume some complex computation
            Thread.sleep(100)
            if (batch.size >= batchSize) {
              println(s"New batch: ${batch.dequeueAll(_ => true).mkString("[", ",", "]")}")
            }

            // send demand upstream
            pull(inPort)
          }

          override def onUpstreamFinish(): Unit = {
            if (batch.nonEmpty) {
              println(s"New batch: ${batch.dequeueAll(_ => true).mkString("[", ",", "]")}")
              println("Stream finished.")
            }
          }
        })
      }

    override def shape: SinkShape[Int] = SinkShape[Int](inPort)
  }

  val batcherSink = Sink.fromGraph(new Batcher(10))

  randomNumberGenerator.to(batcherSink)
//    .run()

  /**
   * Exercise: a custom flow - a simple filter flow
   * - 2 ports: an input port and an output port
   */
  class SimpleFlow[T](predicate: T => Boolean) extends GraphStage[FlowShape[T, T]] {
    // step 1: define the ports and the component-specific members
    val inPort = Inlet[T]("in")
    val outPort = Outlet[T]("out")

    // step 2: construct a new shape
    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)

    // step 3: create the logic
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandler(outPort, new OutHandler {
          // when there is demand from downstream
          override def onPull(): Unit = pull(inPort)
        })

        setHandler(inPort, new InHandler {
          // when the upstream wants to send me an element
          override def onPush(): Unit = {
            try {
              val nextElement = grab(inPort)

              if (predicate(nextElement)) {
                push(outPort, nextElement)
              } else {
                pull(inPort)
              }
            } catch {
              case e: Throwable => failStage(e)
            }
          }
        })
      }
  }

  val myFilter = Flow.fromGraph(new SimpleFlow[Int](_ % 2 == 0))

  randomNumberGenerator
    .via(myFilter)
    .to(batcherSink)
    .run()

  /**
   * Materialized values in graph stages
   */
  // 3 - a flow that counts the number of elements that go through it
  class CounterFlow[T] extends GraphStageWithMaterializedValue[FlowShape[T, T], Future[Int]] {
    val inPort = Inlet[T]("counterInt")
    val outPort = Outlet[T]("counterOut")

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
      val promise = Promise[Int]
      val logic = new GraphStageLogic(shape) {
        // setting mutable stage
        var counter = 0

        setHandler(outPort, new OutHandler {
          override def onPull(): Unit = pull(inPort)

          override def onDownstreamFinish(): Unit = {
            promise.success(counter)
            super.onDownstreamFinish()
          }
        })

        setHandler(inPort, new InHandler {
          override def onPush(): Unit = {
            try {
              val nextElement = grab(inPort)

              counter += 1
              push(outPort, nextElement)
            } catch {
              case e: Throwable => failStage(e)
            }
          }

          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
        })
      }

      (logic, promise.future)
    }

    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)
  }

  val counterFlow = Flow.fromGraph(new CounterFlow[Int])

  val countFuture = Source(1 to 10)
    .viaMat(counterFlow)(Keep.right)
    .to(Sink.foreach[Int](println))
    .run()

  countFuture.onComplete {
    case Success(count) => println(s"The number of elements passed: $count")
    case Failure(ex) => println(s"counting the elements failed: $ex")
  }
}
