package learn_akka_http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, MissingQueryParamRejection, Rejection, RejectionHandler}
import akka.stream.ActorMaterializer

object HandlingRejections extends App {
  implicit val system = ActorSystem("HandlingRejections")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val simpleRoute =
    path("api" / "myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
        parameter(Symbol("id")) { _ =>
          complete(StatusCodes.OK)
        }
    }

  // rejection handler
  val badRequestHandler: RejectionHandler = { (rejections: Seq[Rejection]) =>
    println(s"I have encountered rejections: $rejections")
    Some(complete((StatusCodes.BadRequest)))
  }

  val forbiddenHandler: RejectionHandler = { (rejections: Seq[Rejection]) =>
    println(s"I have encountered rejections: $rejections")
    Some(complete((StatusCodes.Forbidden)))
  }

  val simpleRouteWithHandlers =
    handleRejections(badRequestHandler) { // handle rejections from the top level
      // define server logic inside
      path("api" / "myEndpoint") {
        get {
          complete(StatusCodes.OK)
        } ~
          post {
            handleRejections(forbiddenHandler) { // handle rejections WITHIN
              parameter(Symbol("myParam")) { _ =>
                complete(StatusCodes.OK)
              }
            }
          }
      }
    }

//  RejectionHandler.default
//  Http().bindAndHandle(simpleRouteWithHandlers, "localhost", 8080)

  implicit val customRejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case m: MissingQueryParamRejection =>
        println(s"I got a query param rejection: $m")
        complete("Rejected query param!")
    }
    .handle {
      case m: MethodRejection =>
        println(s"I got a method rejection: $m")
        complete("Rejected method!")
    }
    .result()

  Http().bindAndHandle(simpleRoute, "localhost", 8080)
}
