package learn_akka_http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer

object HandlingExceptions extends App {
  implicit val system = ActorSystem("HandlingExceptions")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val simpleRoute =
    path("api" / "people") {
      get {
        // directive that throws some exceptions
        throw new RuntimeException("Getting all the people took too long")
      } ~
        post {
          parameter(Symbol("id")) { id =>
            if (id.length > 2) {
              throw new NoSuchElementException(
                s"Parameter $id cannot be found in the database, TABLE FLIP!"
              )
            }

            complete(StatusCodes.OK)
          }
        }
    }

  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case g: RuntimeException =>
      complete(StatusCodes.NotFound, g.getMessage)
    case g: IllegalArgumentException =>
      complete(StatusCodes.BadRequest, g.getMessage)
  }

  Http().bindAndHandle(simpleRoute, "localhost", 8080)

  val runtimeExceptionHandler: ExceptionHandler = ExceptionHandler {
    case g: RuntimeException =>
      complete(StatusCodes.NotFound, g.getMessage)
  }

  val noSuchElementExceptionHandler: ExceptionHandler = ExceptionHandler {
    case g: NoSuchElementException =>
      complete(StatusCodes.BadRequest, g.getMessage)
  }

  val delicateHandleRoute =
    handleExceptions(runtimeExceptionHandler) {
      path("api" / "people") {
        get {
          // directive that throws some exceptions
          throw new RuntimeException("Getting all the people took too long")
        } ~
          handleExceptions(noSuchElementExceptionHandler) {
            post {
              parameter(Symbol("id")) { id =>
                if (id.length > 2) {
                  throw new NoSuchElementException(
                    s"Parameter $id cannot be found in the database, TABLE FLIP!"
                  )
                }

                complete(StatusCodes.OK)
              }
            }
          }
      }
    }
}
