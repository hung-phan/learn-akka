name := "akka-typed"

version := "0.1"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,

  // serialization
  "io.altoo" %% "akka-kryo-serialization" % "1.1.0",

  // local levelDB stores
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
