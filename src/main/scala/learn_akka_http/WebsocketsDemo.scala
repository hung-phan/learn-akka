package learn_akka_http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.CompactByteString
import scala.concurrent.duration._
import scala.language.postfixOps

object WebsocketsDemo extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Message: TextMessage vs BinaryMessage
  val textMessage = TextMessage(Source.single("hello via a text message"))
  val binaryMessage = BinaryMessage(
    Source.single(CompactByteString("hello via a binary message"))
  )

  val html =
    """
      |<!DOCTYPE html>
      |<html lang="en">
      |<head>
      |    <meta charset="UTF-8">
      |    <title>Hello websocket</title>
      |</head>
      |<body>
      |<div id="content"></div>
      |<script>
      |  const exampleSocket = new WebSocket("ws://localhost:8080/greeter");
      |
      |  console.log("starting websocket...");
      |
      |  exampleSocket.onmessage = event => {
      |    const newChild = document.createElement("div");
      |
      |    newChild.innerText = event.data;
      |
      |    document.getElementById("content").appendChild(newChild)
      |  };
      |
      |  exampleSocket.onopen = event => {
      |    exampleSocket.send("socket seems to be open...");
      |  };
      |</script>
      |</body>
      |</html>
      |""".stripMargin

  def websocketFlow: Flow[Message, Message, Any] = Flow[Message].map {
    case tm: TextMessage =>
      TextMessage(
        Source.single("Server say back: ") ++ tm.textStream ++ Source
          .single("!")
      )

    case bm: BinaryMessage =>
      // drain content to avoid the stream being clogged
      bm.dataStream.runWith(Sink.ignore)
      TextMessage(Source.single("Server received a binary message..."))
  }

  val websocketRoute =
    (pathEndOrSingleSlash & get) {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
    } ~
      path("greeter") {
        handleWebSocketMessages(socialFlow)
      }

  Http().bindAndHandle(websocketRoute, "localhost", 8080)

  case class SocialPost(owner: String, content: String)

  val socialFeed = Source(
    List(
      SocialPost("Martin", "Scala 3 has been announced!"),
      SocialPost("Daniel", "A new Rock the JVM course is open!"),
      SocialPost("Martin", "I killed Java."),
    )
  )
  val socialMessages = socialFeed
    .throttle(1, 2 seconds)
    .map(
      socialPost =>
        TextMessage(s"${socialPost.owner} said: ${socialPost.content}")
    )

  val socialFlow: Flow[Message, Message, Any] =
    Flow.fromSinkAndSource(Sink.foreach[Message](println), socialMessages)
}
