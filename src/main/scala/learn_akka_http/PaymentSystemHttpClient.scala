package learn_akka_http

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

case class CreditCard(serialNumber: String,
                      securityCode: String,
                      account: String)

object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard,
                            receiverAccount: String,
                            amount: Double)
  case object PaymentAccepted
  case object PaymentRejected
}

trait PaymentJsonProtocol extends DefaultJsonProtocol {
  implicit val creditCardFormat = jsonFormat3(CreditCard)
  implicit val paymentRequestFormat = jsonFormat3(
    PaymentSystemDomain.PaymentRequest
  )
}

class PaymentValidator extends Actor with ActorLogging {
  import PaymentSystemDomain._
  override def receive: Receive = {
    case PaymentRequest(
        CreditCard(serialNumber, _, senderAccount),
        receiverAccount,
        amount
        ) =>
      log.info(
        s"$senderAccount is trying to send $amount dollars to $receiverAccount"
      )

      if (serialNumber == "1234-1234-1234-1234") sender() ! PaymentRejected
      else sender() ! PaymentAccepted
  }
}

object PaymentSystemHttpClient
    extends App
    with PaymentJsonProtocol
    with SprayJsonSupport {
  // micro-service for payment
  implicit val system = ActorSystem("PaymentSystem")
  implicit val materializer = ActorMaterializer()

  import PaymentSystemDomain._
  import system.dispatcher

  val paymentValidator =
    system.actorOf(Props[PaymentValidator], "paymentValidator")

  implicit val timeout = Timeout(2 seconds)

  val paymentRoute =
    path("api" / "payments") {
      post {
        entity(as[PaymentRequest]) { paymentRequest =>
          complete((paymentValidator ? paymentRequest).map {
            case PaymentAccepted => StatusCodes.OK
            case PaymentRejected => StatusCodes.Forbidden
            case _               => StatusCodes.BadRequest
          })
        }
      }
    }

  Http().bindAndHandle(paymentRoute, "localhost", 8080)
}
