package learn_akka_http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object HighLevelExercise extends App with DefaultJsonProtocol {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  /**
    * Exercise
    *
    * - GET /api/people: retrieve all the people you have registered
    * - GET /api/people/pin: retrieve the person with that PIN, return as a JSON
    * - GET /api/people?pin=X (same)
    * - (harder) POST /api/people with a JSON payload denoting a Person, add that person to your database
    *   - extract the HTTP request's payload (entity)
    *     - extract the request
    *     - process the entity's data
    */
  case class Person(pin: Int, name: String)

  implicit val personFormatter = jsonFormat2(Person)

  def toHttpEntity(payload: String) =
    HttpEntity(ContentTypes.`application/json`, payload)

  var people = List(Person(1, "Alice"), Person(2, "Bob"), Person(3, "Charlie"))

  val peopleRoute =
    pathPrefix("api" / "people") {
      get {
        (path(IntNumber) | parameter(Symbol("pin").as[Int])) { pin =>
          complete(
            toHttpEntity(
              people
                .find(_.pin == pin)
                .toJson
                .prettyPrint
            )
          )
        } ~
          pathEndOrSingleSlash {
            complete(toHttpEntity(people.toJson.prettyPrint))
          }
      } ~
        (post & pathEndOrSingleSlash & extractRequestEntity & extractLog) {
          (entity, log) =>
            val personFuture = entity
              .toStrict(3 seconds)
              .map(_.data.utf8String.parseJson.convertTo[Person])

            personFuture.onComplete {
              case Success(person) =>
                log.info(s"Got person: $person")
              case Failure(exception) =>
                log.warning(
                  s"Something failed with fetching the person from the entity: $exception"
                )
            }

            complete(
              personFuture
                .map(_ => StatusCodes.OK)
                .recover {
                  case _ => StatusCodes.InternalServerError
                }
            )
        }
    }

  Http().bindAndHandle(peopleRoute, "localhost", 8080)
}
