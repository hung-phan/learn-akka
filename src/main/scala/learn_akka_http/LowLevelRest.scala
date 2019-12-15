package learn_akka_http

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import learn_akka_http.GuitarDB.{CreateGuitar, FindAllGuitars, GuitarCreated}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
// step 1
import spray.json._

case class Guitar(make: String, model: String, quantity: Int = 0)

object GuitarDB {

  case class CreateGuitar(guitar: Guitar)

  case class AddGuitar(id: Int, quantity: Int)

  case class GuitarCreated(id: Int)

  case class GuitarAdded(id: Int)

  case class FindGuitar(id: Int)

  case class FindAllGuitarsInStock(inStock: Boolean)

  case object FindAllGuitars

}

class GuitarDB extends Actor with ActorLogging {

  import GuitarDB._

  var guitars: mutable.Map[Int, Guitar] = mutable.Map()
  var currentGuitarId = 0

  override def receive: Receive = {
    case FindAllGuitars =>
      log.info("Searching for all guitars")
      sender() ! guitars.values.toList

    case FindAllGuitarsInStock(inStock) =>
      log.info(s"Searching for that in stock: $inStock")
      sender() ! guitars.values.filter { guitar =>
        if (inStock)
          guitar.quantity > 0
        else
          guitar.quantity <= 0
      }.toList

    case FindGuitar(id) =>
      log.info(s"Searching guitar by id: $id")
      sender() ! guitars.get(id)

    case AddGuitar(id, quantity) =>
      log.info("Adding more guitars")

      if (guitars.contains(id)) {
        val guitar = guitars(id)

        guitars(id) = guitar.copy(quantity = guitar.quantity + quantity)

        sender() ! GuitarAdded(id)
      } else {
        sender() ! GuitarAdded(-1)
      }

    case CreateGuitar(guitar) =>
      log.info(s"Adding guitar $guitar with id $currentGuitarId")
      guitars += (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
  }
}

// step 2
trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  // step 3
  implicit val guitarFormat = jsonFormat3(Guitar)
}

object LowLevelRest extends App with GuitarStoreJsonProtocol {
  implicit val system = ActorSystem("LowLevelRest")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  /**
   * GET localhost:8080/api/guitar => All the guitars in the store
   * GET localhost:8080/api/guitar?id=X => Fetch the guitar associated with id X
   * POST on localhost:8080/api/guitar => Insert the guitar into the store
   */

  // JSON -> marshalling
  val simpleGuitar = Guitar("Fender", "Stratocaster")

  println(simpleGuitar.toJson.prettyPrint)

  // unmarshalling
  val simpleGuitarJsonString = simpleGuitar.toJson.prettyPrint

  println(simpleGuitarJsonString.parseJson.convertTo[Guitar])

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

  /**
   * server code
   */
  implicit val defaultTimeout = Timeout(2 seconds)

  import GuitarDB._

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>

      /**
       * query parameter handling code
       */
      val query = uri.query() // query object <=> Map[String, String]

      if (query.isEmpty) {
        val guitarsFuture = (guitarDb ? FindAllGuitarsInStock).mapTo[List[Guitar]]

        guitarsFuture.map { guitars =>
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.compactPrint
            )
          )
        }
      } else {
        // fetch guitar associated to the guitar id
        // localhost:8080/api/guitar?id=45
        // be careful for this operation. It can throw error on production
        val guitarId = query.get("id").map(_.toInt) // Option[String]

        guitarId match {
          case Some(id: Int) =>
            val guitarFuture = (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]]

            guitarFuture.map {
              case Some(guitar) => HttpResponse(
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  guitar.toJson.prettyPrint
                )
              )
              case None => HttpResponse(StatusCodes.NotFound)
            }

          case None => Future(HttpResponse(StatusCodes.NotFound))
        }
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      // entities are a Source[ByteString, Any]
      val strictEntityFuture = entity.toStrict(3 seconds)

      strictEntityFuture.flatMap { strictEntity =>
        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]

        val guitarCreatedFuture: Future[GuitarCreated] = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]

        guitarCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }
      }

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>

      /**
       * query parameter handling code
       */
      val query = uri.query() // query object <=> Map[String, String]

      if (query.isEmpty) {
        Future(
          HttpResponse(StatusCodes.BadRequest)
        )
      } else {
        val inStock = query.get("inStock").map(_.toBoolean) // Option[Boolean]

        inStock match {
          case Some(inStockVal) =>
            val inStockGuitarsFuture = (guitarDb ? FindAllGuitarsInStock(inStockVal)).mapTo[List[Guitar]]

            inStockGuitarsFuture.map { guitars =>
              HttpResponse(
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  guitars.toJson.prettyPrint
                )
              )
            }

          case None => Future(HttpResponse(StatusCodes.BadRequest))
        }
      }

    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>

      /**
       * query parameter handling code
       */
      val query = uri.query() // query object <=> Map[String, String]

      if (query.isEmpty) {
        Future(
          HttpResponse(StatusCodes.BadRequest)
        )
      } else {
        val guitarId = query.get("id").map(_.toInt) // Option[Int]
        val guitarQuantity = query.get("quantity").map(_.toInt) // Option[Int]

        val validGuitarResponseFuture: Option[Future[HttpResponse]] = for {
          id <- guitarId
          quantity <- guitarQuantity
        } yield {
          val guitarAddedFuture = (guitarDb ? AddGuitar(id, quantity)).mapTo[GuitarAdded]

          guitarAddedFuture.map(_ => HttpResponse(StatusCodes.OK))
        }

        validGuitarResponseFuture.getOrElse(Future(HttpResponse(StatusCodes.BadRequest)))
      }

    case request: HttpRequest =>
      request.discardEntityBytes()

      Future {
        HttpResponse(StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

  /**
   * Exercise: enhance the Guitar case class with a quantity field, by default 0
   * - GET to /api/guitar/inventory?inStock=true,false which returns the guitars in stock as a JSON
   * - POST to /api/guitar/inventory?id=X?quantity=Y which adds Y guitars to the stock for guitar with id X
   */
}
