package learn_akka_serialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import learn_akka_remoting.SimpleActor

case class Book(title: String, year: Int)

object KryoSerialization_Local extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2551")
    .withFallback(
      ConfigFactory.load("akka_serialization/kryo_serialization.conf")
    )
  val system = ActorSystem("LocalSystem", config)
  val actorSelection =
    system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")

  actorSelection ! Book("The Rock the JVM Experience", 2019)
}

object KryoSerialization_Remote extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2552")
    .withFallback(
      ConfigFactory.load("akka_serialization/kryo_serialization.conf")
    )
  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor")
}

object KryoSerialization_Persistence extends App {
  val config = ConfigFactory
    .load("akka_serialization/kryo_serialization.conf")
    .getConfig("postgresDemo")
    .withFallback(
      ConfigFactory.load("akka_serialization/kryo_serialization.conf")
    )
  val system = ActorSystem("PersistenceSystem", config)
  val simplePersistentActor =
    system.actorOf(
      SimplePersistentActor.props("kryo-actor"),
      "kryoBookActor"
    )

  simplePersistentActor ! Book("The Rock the JVM Experience", 2019)
}

