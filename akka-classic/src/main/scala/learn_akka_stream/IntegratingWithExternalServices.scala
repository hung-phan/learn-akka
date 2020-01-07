package learn_akka_stream

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object IntegratingWithExternalServices extends App {
  implicit val system = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer = ActorMaterializer()

//  import system.dispatcher // not recommended for mapAsync
  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  def genericExtService[A, B](element: A): Future[B] = ???

  // example: simplified PagerDuty
  case class PagerEvent(application: String, description: String, date: Date)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "Infrastructure broke", new Date),
    PagerEvent("FastDataPipeline", "Illegal elements in the data pipeline", new Date),
    PagerEvent("AkkaInfra", "A service stopped responding", new Date),
    PagerEvent("SuperFrontend", "A button doesn't work", new Date),
  ))

  class PagerActor extends Actor with ActorLogging {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rockthejvm.com",
      "John" -> "john@rockthejvm.com",
      "Lady Gaga" -> "ladygaga@rockthejvm.com",
    )

    private def processEvent(pagerEvent: PagerEvent) = Future {
      val engineerIndex = pagerEvent.date.toInstant.getEpochSecond / (24 * 3600) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      // page the engineer
      println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")
      Thread.sleep(1000)

      // return the engineer that was paged
      engineerEmail
    }

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)
    }
  }

  val infraEvents = eventSource.filter(_.application == "AkkaInfra")

  val pagedEmailsSink = Sink.foreach[String](email => println(s"Successfully sent notification to $email"))
  // guarantees the relative order of elements
  // also have the method mapAsyncUnordered
//  val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
//  pagedEngineerEmails.to(pagedEmailsSink).run()

  import akka.pattern.ask

  implicit val timeout = Timeout(3 seconds)

  val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")
  val alternativePagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])

  alternativePagedEngineerEmails.to(pagedEmailsSink).run()

  // do not confuse mapAsync with async
}
