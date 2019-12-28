package learn_akka_http

import java.io.File

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}

object UploadingFiles extends App {
  implicit val system = ActorSystem("UploadingFiles")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val filesRoute =
    (pathEndOrSingleSlash & get) {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |<body>
            | <form action="http://localhost:8080/upload" method="post" enctype="multipart/form-data">
            |   <input type="file" name="uploaded_file" />
            |   <button type="submit">Upload</button>
            | </form>
            |</body>
            |</html>
            |""".stripMargin
        )
      )
    } ~
      (path("upload") & extractLog) { log =>
        // handle uploading files
        // multipart/form-data

        entity(as[Multipart.FormData]) { formData =>
          // handle file payload
          val partsSource: Source[Multipart.FormData.BodyPart, Any] =
            formData.parts
          val filePartsSink: Sink[Multipart.FormData.BodyPart, Future[Done]] =
            Sink.foreach[Multipart.FormData.BodyPart] { bodyPart =>
              // create a file
              val filename = "src/main/resources/download/" + bodyPart.filename
                .getOrElse("tempFile_" + System.currentTimeMillis())
              val file = new File(filename)

              log.info(s"Writing to file: $filename")

              val fileContentsSource: Source[ByteString, _] =
                bodyPart.entity.dataBytes
              val fileContentsSink: Sink[ByteString, _] =
                FileIO.toPath(file.toPath)

              fileContentsSource.runWith(fileContentsSink)
            }
          val writeOperationFuture = partsSource.runWith(filePartsSink)

          onComplete(writeOperationFuture) {
            case Success(_) => complete("File uploaded.")
            case Failure(exception) =>
              complete(s"File failed to upload: $exception")
          }
        }
      }

  Http().bindAndHandle(filesRoute, "localhost", 8080)
}
