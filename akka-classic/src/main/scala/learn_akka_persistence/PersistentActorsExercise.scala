package learn_akka_persistence

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object PersistentActorsExercise extends App {

  /**
   * Persistent actor for a voting station
   * Keep:
   * - the citizens who voted
   * - the poll: mapping between a candidate and the number of received votes so far
   *
   * The actor must be able to recover its state if it's shutdown or restarted
   */
  case class Vote(citizenPID: String, candidate: String)

  class VotingStation extends PersistentActor with ActorLogging {
    val citizens: mutable.Set[String] = mutable.Set()
    val poll: mutable.Map[String, Int] = mutable.Map()

    override def persistenceId: String = "PollActor"

    override def receiveCommand: Receive = {
      case vote: Vote => {
        if (!citizens.contains(vote.citizenPID)) {
          persist(vote) { e =>
            log.info(s"Persisted: $vote")
            handleInternalStateChange(e)
          }
        }
      }
      case "print" =>
        println(citizens, poll)
    }

    override def receiveRecover: Receive = {
      case vote: Vote =>
        log.info(s"Recovered: $vote")
        handleInternalStateChange(vote)
    }

    def handleInternalStateChange(vote: Vote): Unit = {
      citizens.add(vote.citizenPID)
      val votes = poll.getOrElse(vote.candidate, 0)
      poll.put(vote.candidate, votes + 1)
    }
  }

  val system = ActorSystem("PersistentActorExercise", ConfigFactory.load().getConfig("learn_akka_persistence"))
  val votingStation = system.actorOf(Props[VotingStation])

  val votesMap = Map[String, String](
    "Alice" -> "Martin",
    "Bob" -> "Roland",
    "Charlie" -> "Martin",
    "David" -> "Jonas"
  )

  for ((citizenPID, candidate) <- votesMap)
    votingStation ! Vote(citizenPID, candidate)

  votingStation ! "print"
}
