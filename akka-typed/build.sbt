name := "akka-typed"

version := "0.1"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,

  // akka test kit
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

  "org.scalatest" %% "scalatest" % "3.0.8" % Test,

  // aeron
  "io.aeron" % "aeron-driver" % "1.27.0",
  "io.aeron" % "aeron-client" % "1.27.0",

  // serialization
  "io.altoo" %% "akka-kryo-serialization" % "1.1.0",

  // local levelDB stores
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
