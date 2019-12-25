package learn_akka_http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.Uri.Path.~
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object HighLevelIntro extends App {
  implicit val system = ActorSystem("HighLevelIntro")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") {
      complete(StatusCodes.OK)
    }

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // chaining directives with ~
  val chainedRoute: Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
        post {
          complete((StatusCodes.Forbidden))
        }
    } ~
      path("home") {
        complete(
          HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            """
              |<html>
              |<body>
              |  Hello from the high level Akka HTTP
              |</body>
              |</html>
              |""".stripMargin
          )
        )
      }

  Http().bindAndHandle(pathGetRoute, "localhost", 8080)
}
