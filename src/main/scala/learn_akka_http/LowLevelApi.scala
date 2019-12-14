package learn_akka_http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ContentTypes, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object LowLevelApi extends App {
  implicit val system = ActorSystem("LowLevelServerAPI")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val serverSource = Http().bind("localhost", 8000)
  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Accepted incoming connection from: ${connection.remoteAddress}")
  }

  val serverBindingFuture = serverSource
    .to(connectionSink)
    .run()

  serverBindingFuture.onComplete {
    case Success(binding) =>
      println(s"Server binding successful at ${binding.localAddress}")
      binding.terminate(2 seconds)
    case Failure(exception) => println(s"Server binding failed: $exception")
  }
  /**
   * Method 1: synchronously serve HTTP responses
   */
  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, uri, value, entity, protocol) => HttpResponse(
      StatusCodes.OK, // HTTP 200
      entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |  <body>
          |    Hello from Akka HTTP!
          |  </body>
          |</html>
          |""".stripMargin
      )
    )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    OOPS! The resource can't be found.
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithSyncHandler(requestHandler)
  }

//  Http().bind("localhost", 9000).runWith(httpSyncConnectionHandler)

  // short hand version
//  Http().bindAndHandleSync(requestHandler, "localhost", 9000)
  /**
   * Method 2: Serve back HTTP response ASYNCHRONOUSLY
   */
  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), value, entity, protocol) => Future(HttpResponse(
      StatusCodes.OK, // HTTP 200
      entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |  <body>
          |    Hello from Akka HTTP!
          |  </body>
          |</html>
          |""".stripMargin
      )
    ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    OOPS! The resource can't be found.
            |  </body>
            |</html>
            |""".stripMargin
        )
      ))
  }

  // short hand version
//  Http().bindAndHandleAsync(asyncRequestHandler, "localhost", 9000)

  /**
   * Method 3: async via Akka streams
   */
  val streamBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), value, entity, protocol) => HttpResponse(
      StatusCodes.OK, // HTTP 200
      entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |  <body>
          |    Hello from Akka HTTP!
          |  </body>
          |</html>
          |""".stripMargin
      )
    )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    OOPS! The resource can't be found.
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
  }

//  Http().bind("localhost", 8082).runForeach( connection =>
//    connection.handleWith(streamBasedRequestHandler)
//  )

  // short hand version
//  Http().bindAndHandle(streamBasedRequestHandler, "localhost", 8082)

  /**
   * Exercise: create your own HTTP server running on localhost on 9000, which replies
   * - with a welcome message on the "front door" localhost:9000
   * - with a proper HTML on localhost:8388/about
   * - with a 404 message otherwise
   */
  val exerciseRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), value, entity, protocol) => HttpResponse(
      StatusCodes.OK, // HTTP 200
      entity = HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "front door"
      )
    )
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), value, entity, protocol) => HttpResponse(
      StatusCodes.OK, // HTTP 200
      entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        """
          |<html>
          |  <body>
          |    This is the about page
          |  </body>
          |</html>
          |""".stripMargin
      )
    )
    // path /search redirects to some other part of
    case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) => HttpResponse(
      StatusCodes.Found,
      headers = List(Location("http://google.com"))
    )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    OOPS! The resource can't be found.
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  //  Http().bind("localhost", 8082).runForeach( connection =>
  //    connection.handleWith(streamBasedRequestHandler)
  //  )

  // short hand version
  val bindingFuture = Http().bindAndHandle(exerciseRequestHandler, "localhost", 9000)

  // shutdown the server:
  bindingFuture
    .flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}
