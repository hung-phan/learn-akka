package learn_akka_http

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import learn_akka_http.GuitarDB._

import scala.concurrent.duration._
import scala.language.postfixOps
import spray.json._

import scala.concurrent.Future

object HighLevelExample extends App with GuitarStoreJsonProtocol {
  implicit val system = ActorSystem("HighLevelExample")
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(2 seconds)

  import system.dispatcher

  /**
   * GET /api/guitar fetches ALL the guitars in the store
   * GET /api/guitar?id=x fetches the guitar with id X
   * GET /api/guitar/X fetches guitar with id X
   * GET /api/guitar/inventory?inStock=true|false
   */

  /**
   * setup
   */
  val guitarDb = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")

  val guitarList = List(
    Guitar("Fender", "Stratocaster", 1),
    Guitar("Gibson", "Les Paul", 1),
    Guitar("Martin", "LX1", 1)
  )

  guitarList.foreach(guitarDb ! CreateGuitar(_))

  val guitarServerRoute =
    pathPrefix("api" / "guitar") {
      // ALWAYS PUT THE SPECIFIC ROUTE FIRST
      pathPrefix("inventory") {
        get {
          parameter(Symbol("inStock").as[Boolean]) { (inStock: Boolean) =>
            val guitarsFuture = (guitarDb ? FindAllGuitarsInStock(inStock)).mapTo[List[Guitar]]
            val entityFuture = guitarsFuture.map { guitars =>
              HttpEntity(
                ContentTypes.`application/json`,
                guitars.toJson.prettyPrint
              )
            }

            complete(entityFuture)
          }
        }
      } ~ (parameter(Symbol("id").as[Int]) | pathPrefix(IntNumber)) { guitarId =>
        get {
          val guitarFuture: Future[Option[Guitar]] = (guitarDb ? FindGuitar(guitarId)).mapTo[Option[Guitar]]
          val entityFuture = guitarFuture.map { guitarOption =>
            HttpEntity(
              ContentTypes.`application/json`,
              guitarOption.toJson.prettyPrint
            )
          }

          complete(entityFuture)
        }
      } ~
        get {
          val guitarsFuture = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
          val entityFuture = guitarsFuture.map { guitars =>
            HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          }

          complete(entityFuture)
        }
    }

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val simplifiedGuitarServerRoute =
    (pathPrefix("api" / "guitar") & get) {
      path("inventory") {
        // inventory logic
        parameter(Symbol("inStock").as[Boolean]) { (inStock: Boolean) =>
          complete(
            (guitarDb ? FindAllGuitarsInStock(inStock))
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
      } ~
        (path(IntNumber) | parameter(Symbol("id").as[Int])) { guitarId =>
          complete(
            (guitarDb ? FindGuitar(guitarId))
              .mapTo[Option[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        } ~
        pathEndOrSingleSlash {
          complete(
            (guitarDb ? FindAllGuitars)
              .mapTo[List[Guitar]]
              .map(_.toJson.prettyPrint)
              .map(toHttpEntity)
          )
        }
    }

  Http().bindAndHandle(simplifiedGuitarServerRoute, "localhost", 8080)
}
