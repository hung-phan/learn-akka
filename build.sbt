name := "udemy-akka-essentials"

version := "0.1"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.5.25"
lazy val postgresVersion = "42.2.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

  "org.scalatest" %% "scalatest" % "3.2.0-M1" % Test,

  // local levelDB stores
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  // JDBC with PostgresSQL
  "org.postgresql" % "postgresql" % postgresVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.2"
)
