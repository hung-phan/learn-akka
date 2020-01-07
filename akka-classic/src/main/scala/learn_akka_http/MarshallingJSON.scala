package learn_akka_http

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

import scala.language.postfixOps
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

// step 1
import spray.json._

case class Player(nickname: String, characterClass: String, level: Int)

class GameAreaMap extends Actor with ActorLogging {
  import GameAreaMap._

  var players = Map[String, Player]()

  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("Getting all players")
      sender() ! players.values.toList

    case GetPlayer(nickname) =>
      log.info(s"Getting player with nickname: $nickname")
      sender() ! players.get(nickname)

    case GetPlayersByClass(characterClass) =>
      log.info(s"Getting all players with the character class: $characterClass")
      sender() ! players.values.toList
        .filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      log.info(s"Trying to add player $player")
      players += (player.nickname -> player)
      sender() ! OperationSuccess

    case RemovePlayer(player) =>
      log.info(s"Trying to remove $player")
      players -= player.nickname
  }
}

object GameAreaMap {
  case object GetAllPlayers
  case class GetPlayer(nickname: String)
  case class GetPlayersByClass(characterClass: String)
  case class AddPlayer(player: Player)
  case class RemovePlayer(player: Player)
  case object OperationSuccess
}

// step 2
trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val playerFormat = jsonFormat3(Player)
}

object MarshallingJSON
    extends App
    // step 3
    with PlayerJsonProtocol
    with SprayJsonSupport {
  implicit val system = ActorSystem("MarshallingJSON")
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(2 seconds)

  import GameAreaMap._
  import system.dispatcher

  val rtjvmGameMap = system.actorOf(Props[GameAreaMap], "rockTheJVMGameAreaMap")
  val playersList = List(
    Player("martin killz u", "Warrior", 70),
    Player("rolandbraveheart007", "Elf", 67),
    Player("daniel_rock_03", "Wizard", 30)
  )

  playersList.foreach { player =>
    rtjvmGameMap ! AddPlayer(player)
  }

  /**
    * - GET /api/player returns all the players in the map, as JSON
    * - GET /api/player/(nickname) returns the player with the given nickname (as JSON)
    * - GET /api/player?nickname=X, does the same
    * - GET /api/player/class/(characterClass) returns all the players with the given character class
    * - POST /api/player with JSON payload, adds the player to the map
    * - DELETE /api/player with JSON payload, removes the player from the map
    */
  val rtjvmGameRouteSkeleton =
    pathPrefix("api" / "player") {
      get {
        path("class" / Segment) { characterClass =>
          complete(
            (rtjvmGameMap ? GetPlayersByClass(characterClass))
              .mapTo[List[Player]]
          )
        } ~
          (path(Segment) | parameter(Symbol("nickname"))) { nickname =>
            complete((rtjvmGameMap ? GetPlayer(nickname)).mapTo[Option[Player]])
          } ~
          pathEndOrSingleSlash {
            complete((rtjvmGameMap ? GetAllPlayers).mapTo[List[Player]])
          }
      } ~
        post {
          // entity(implicitly[FromRequestUnmarshaller[Player]])
          entity(as[Player]) { player =>
            complete((rtjvmGameMap ? AddPlayer(player)).map(_ => StatusCodes.OK))
          }
        } ~
        delete {
          entity(as[Player]) { player =>
            complete((rtjvmGameMap ? RemovePlayer(player)).map(_ => StatusCodes.OK))
          }
        }
    }
}
