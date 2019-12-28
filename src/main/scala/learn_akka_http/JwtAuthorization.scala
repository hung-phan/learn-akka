package learn_akka_http

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}
import spray.json._

import scala.util.{Failure, Success}

object SecurityDomain extends DefaultJsonProtocol {
  case class LoginRequest(username: String, password: String)

  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
}

object JwtAuthorization extends App with SprayJsonSupport {
  implicit val system = ActorSystem("JwtAuthorization")
  implicit val materializer = ActorMaterializer()

  import SecurityDomain._
  import system.dispatcher

  val superSecretPasswordDb =
    Map("admin" -> "admin", "daniel" -> "Rockthejvm1!")

  val algorithm = JwtAlgorithm.HS256
  val secretKey = "rockthejvmsecret"

  def checkPassword(username: String, password: String): Boolean =
    superSecretPasswordDb.contains(username) && superSecretPasswordDb(username) == password

  def createToken(username: String, expirationPeriodInDays: 1): String = {
    val claims = JwtClaim(
      expiration = Some(
        System.currentTimeMillis() / 1000 + TimeUnit.DAYS
          .toSeconds(expirationPeriodInDays)
      ),
      issuedAt = Some(System.currentTimeMillis() / 1000),
      issuer = Some("rockthejvm.com")
    )

    JwtSprayJson.encode(claims, secretKey, algorithm) // JWT string
  }

  def isTokenExpire(token: String): Boolean =
    JwtSprayJson.decode(token, secretKey, Seq(algorithm)) match {
      case Success(claims) =>
        claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
      case Failure(_) => true
    }

  def isTokenValid(token: String): Boolean =
    JwtSprayJson.isValid(token, secretKey, Seq(algorithm))

  val loginRoute =
    post {
      entity(as[LoginRequest]) {
        case LoginRequest(username, password)
            if checkPassword(username, password) =>
          val token = createToken(username, 1)

          respondWithHeader(RawHeader("Access-Token", token)) {
            complete(StatusCodes.OK)
          }
        case _ => complete(StatusCodes.Unauthorized)
      }
    }

  val authenticated =
    (path("secureEndpoint") & get) {
      optionalHeaderValueByName("Authorization") {
        case Some(token) =>
          if (isTokenValid(token)) {
            if (isTokenExpire(token)) {
              complete(
                HttpResponse(
                  status = StatusCodes.Unauthorized,
                  entity = "Token expired."
                )
              )
            } else {
              complete("User accessed authorized endpoint!")
            }
          } else {
            complete(
              HttpResponse(
                status = StatusCodes.Unauthorized,
                entity = "Token is invalid, or has been tampered with."
              )
            )
          }
        case _ =>
          complete(
            HttpResponse(
              status = StatusCodes.Unauthorized,
              entity = "No token provided!"
            )
          )
      }
    }

  val route = loginRoute ~ authenticated

  Http().bindAndHandle(route, "localhost", 8080)
}