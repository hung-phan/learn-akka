package playground

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /*
   * 1. Inline config
   */
  val configString =
    """
      | akka {
      |   loglevel = "DEBUG"
      | }
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "A message to remember"

  /*
   * 2. Config file
   */
  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])

  defaultConfigActor ! "Remember me"

  /*
   * 3. Separate config in the same file
   */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])

  specialConfigActor ! "Special!"

  /*
   * 3. Separate config in another file
   */
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  val separateConfigSystem = ActorSystem("SeparateConfigDemo", separateConfig)
  val separateConfigActor = separateConfigSystem.actorOf(Props[SimpleLoggingActor])

  separateConfigActor ! "Separate config!"
}
