package learn_akka_serialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import learn_akka_remoting.SimpleActor
import learn_akka_serialization.DataModel.OnlineStoreUser

object ProtobufSerialization_Local extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2551")
    .withFallback(
      ConfigFactory.load("akka_serialization/protobuf_serialization.conf")
    )
  val system = ActorSystem("LocalSystem", config)
  val actorSelection =
    system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")

  val onlineStoreUser = OnlineStoreUser
    .newBuilder()
    .setUserId(45621)
    .setUserName("colorvisa")
    .setUserEmail("colorvisavn@gmail.com")
    .build()

  actorSelection ! onlineStoreUser
}

object ProtobufSerialization_Remote extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2552")
    .withFallback(
      ConfigFactory.load("akka_serialization/protobuf_serialization.conf")
    )
  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor")
}

object ProtobufSerialization_Persistence extends App {
  val config = ConfigFactory
    .load("akka_serialization/protobuf_serialization.conf")
    .getConfig("postgresDemo")
    .withFallback(
      ConfigFactory.load("akka_serialization/protobuf_serialization.conf")
    )
  val system = ActorSystem("PersistenceSystem", config)
  val simplePersistentActor =
    system.actorOf(
      SimplePersistentActor.props("protobuf-actor"),
      "protobufActor"
    )
  val onlineStoreUser = OnlineStoreUser
    .newBuilder()
    .setUserId(45621)
    .setUserName("colorvisa")
    .setUserEmail("colorvisavn@gmail.com")
    .build()

  simplePersistentActor ! onlineStoreUser
}
