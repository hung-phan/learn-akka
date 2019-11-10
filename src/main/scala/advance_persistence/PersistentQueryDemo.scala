package advance_persistence

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.{Offset, PersistenceQuery}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object PersistentQueryDemo extends App {
  val system = ActorSystem("PersistenceQueryDemo", ConfigFactory.load().getConfig("persistenceQuery"))

  // read journal
  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  // give me all persistence IDs
  val persistenceIds = readJournal.persistenceIds()
  //  val persistenceIds = readJournal.currentPersistenceIds()

  implicit val materializer = ActorMaterializer()(system)

  //  persistenceIds.runForeach { persistenceId =>
  //    println(s"Found persistence ID: $persistenceId")
  //  }

  class SimplePersistentActor extends PersistentActor with ActorLogging {
    override def persistenceId: String = "persistence-query-id-1"

    override def receiveCommand: Receive = {
      case message => persist(message) { _ =>
        log.info(s"Persisted: $message")
      }
    }

    override def receiveRecover: Receive = {
      case e => log.info(s"Recovered: $e")
    }
  }

  val simpleActor = system.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

  import system.dispatcher

  //  system.scheduler.scheduleOnce(5 seconds) {
  //    simpleActor ! "Hello there"
  //  }

  // events by persistence ID
  val events = readJournal.eventsByPersistenceId("persistence-query-id-1", 0, Long.MaxValue)
  //  val events = readJournal.currentEventsByPersistenceId("persistence-query-id-1", 0, Long.MaxValue)

  events.runForeach { event =>
    println(s"Read event: $event")
  }

  // events by tags
  val genres = Array("pop", "rock", "hip-hop", "jazz", "disco")

  case class Song(artist: String, title: String, genre: String)

  // command
  case class PlayList(songs: List[Song])

  // event
  case class PlaylistPurchased(id: Int, songs: List[Song])

  class MusicStoreCheckoutActor extends PersistentActor with ActorLogging {
    var latestPlaylistId = 0

    override def persistenceId: String = "music-store-checkout"

    override def receiveCommand: Receive = {
      case PlayList(songs) =>
        persist(PlaylistPurchased(latestPlaylistId, songs)) { _ =>
          log.info(s"User purchased: $songs")
          latestPlaylistId += 1
        }
    }

    override def receiveRecover: Receive = {
      case event@PlaylistPurchased(id, _) =>
        log.info(s"Recovered: $event")
        latestPlaylistId = id
    }
  }

  class MusicStoreEventAdapter extends WriteEventAdapter {
    override def manifest(event: Any): String = "musicStore"

    override def toJournal(event: Any): Any = event match {
      case event@PlaylistPurchased(_, songs) =>
        val genres = songs.map(_.genre).toSet

        Tagged(event, genres)
      case other => other
    }
  }

  val checkoutActor = system.actorOf(Props[MusicStoreCheckoutActor], "musicStoreActor")
  val r = new Random()

  for (_ <- 1 to 10) {
    val maxSongs = r.nextInt(genres.length)
    val songs = for (i <- 1 to maxSongs) yield {
      val randomGenre = genres(r.nextInt(genres.length))

      Song(s"Artist $i", s"My love song $i", randomGenre)
    }

    checkoutActor ! PlayList(songs.toList)
  }

  val rockPlaylists = readJournal.eventsByTag("rock", Offset.noOffset)
  rockPlaylists.runForeach { event =>
    println(s"Found a playlist with a rock song: $event")
  }
}

