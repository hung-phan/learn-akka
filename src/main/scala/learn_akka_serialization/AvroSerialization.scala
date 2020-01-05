package learn_akka_serialization

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import akka.actor.{ActorSystem, Props}
import akka.serialization.Serializer
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream, AvroSchema}
import com.typesafe.config.ConfigFactory
import learn_akka_remoting.SimpleActor

import scala.util.Using

case class BankAccount(iban: String,
                       bankCode: String,
                       amount: Double,
                       currency: String)
case class CompanyRegistry(name: String,
                           accounts: Seq[BankAccount],
                           activityCode: String,
                           marketCap: Double)

class CompanyRegistrySerializer extends Serializer {
  val companyRegistrySchema = AvroSchema[CompanyRegistry]

  override def identifier: Int = 75411

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case c: CompanyRegistry =>
      val baos = new ByteArrayOutputStream()

      Using(
        AvroOutputStream
          .binary[CompanyRegistry]
          .to(baos)
          .build(companyRegistrySchema)
      ) { avroOutputStream =>
        avroOutputStream.write(c)
        avroOutputStream.flush()
      }

      baos.toByteArray
    case _ =>
      throw new IllegalArgumentException(
        "We only support company registries for Avro"
      )
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte],
                          manifest: Option[Class[_]]): AnyRef = {
    var companyRegistry: CompanyRegistry = null

    Using(
      AvroInputStream
        .binary[CompanyRegistry]
        .from(new ByteArrayInputStream(bytes))
        .build(companyRegistrySchema)
    ) { inputStream =>
      val companyRegistryIterator = inputStream.iterator

      companyRegistry = companyRegistryIterator.next()
    }

    println(s"Deserializing $companyRegistry")

    companyRegistry
  }
}

object AvroSerialization_Local extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2551")
    .withFallback(
      ConfigFactory.load("akka_serialization/avro_serialization.conf")
    )
  val system = ActorSystem("LocalSystem", config)
  val actorSelection =
    system.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteActor")

  actorSelection ! CompanyRegistry(
    "Google",
    Seq(
      BankAccount("US-1234", "google-bank", 4.3, "gazillion dollars"),
      BankAccount("GB-4321", "google-bank", 0.7, "trillion pounds")
    ),
    "ads",
    523895
  )
}

object AvroSerialization_Remote extends App {
  val config = ConfigFactory
    .parseString("akka.remote.artery.canonical.port = 2552")
    .withFallback(
      ConfigFactory.load("akka_serialization/avro_serialization.conf")
    )
  val system = ActorSystem("RemoteSystem", config)
  val simpleActor = system.actorOf(Props[SimpleActor], "remoteActor")
}

object AvroSerialization_Persistence extends App {
  val config = ConfigFactory
    .load("akka_serialization/avro_serialization.conf")
    .getConfig("postgresDemo")
    .withFallback(
      ConfigFactory.load("akka_serialization/avro_serialization.conf")
    )
  val system = ActorSystem("PersistenceSystem", config)
  val simplePersistentActor =
    system.actorOf(SimplePersistentActor.props("avro-actor"), "avroActor")

  simplePersistentActor ! CompanyRegistry(
    "Google",
    Seq(
      BankAccount("US-7892", "google-bank", 4.3, "gazillion dollars"),
      BankAccount("GB-2123", "google-bank", 0.7, "trillion pounds"),
    ),
    "ads",
    523895
  )
}

object SimpleAvroApp extends App {
  println(AvroSchema[CompanyRegistry])
}
