package learn_akka_serialization

import akka.actor.{ActorSystem, Props}
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory
import learn_akka_remoting.SimpleActor
import spray.json._

case class Person(name: String, age: Int)

class PersonSerializer extends Serializer {
  val SEPARATOR = "//"
  override def identifier: Int = 74238

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case person @ Person(name, age) =>
      println(s"Serializing $person")

      s"[$name$SEPARATOR$age]".getBytes
    case _ =>
      throw new IllegalArgumentException(
        "only Person is supported for this serializer"
      )
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte],
                          manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val Array(name, age) =
      string.substring(1, string.length - 1).split(SEPARATOR)
    val person = Person(name, age.toInt)

    println(s"Deserialized $person")

    person
  }
}

class PersonJsonSerializer extends Serializer with DefaultJsonProtocol {
  implicit val personFormat = jsonFormat2(Person)

  override def identifier: Int = 42348

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case p: Person =>
      val json = p.toJson.compactPrint
      println(s"Converting $p to $json")
      json.getBytes
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte],
                          manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val person = string.parseJson.convertTo[Person]
    println(s"Deserialized $string to $person")
    person
  }
}

object CustomSerialization_Local extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2551")
    .withFallback(
      ConfigFactory.load("akka_serialization/custom_serialization.conf")
    )
  val system = ActorSystem("LocalSystem", config)
  val actorSelection =
    system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")

  actorSelection ! Person("Alice", 23)
}

object CustomSerialization_Remote extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2552")
    .withFallback(
      ConfigFactory.load("akka_serialization/custom_serialization.conf")
    )
  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor")
}

object CustomSerialization_Persistence extends App {
  val config = ConfigFactory
    .load("akka_serialization/custom_serialization.conf")
    .getConfig("postgresDemo")
    .withFallback(
      ConfigFactory.load("akka_serialization/custom_serialization.conf")
    )
  val system = ActorSystem("PersistenceSystem", config)
  val simplePersistentActor =
    system.actorOf(
      SimplePersistentActor.props("person-json"),
      "personJsonActor"
    )

  simplePersistentActor ! Person("Alice", 23)
}
