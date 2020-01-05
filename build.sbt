name := "udemy-akka-essentials"

version := "0.1"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.1.11"
lazy val postgresVersion = "42.2.8"
lazy val cassandraVersion = "0.101"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,

  // serialization
  "io.altoo" %% "akka-kryo-serialization" % "1.1.0",
  "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.4",
  "com.google.protobuf" % "protobuf-java" % "3.11.1",

  // support for jwt
  "com.pauldijou" %% "jwt-spray-json" % "4.2.0",

  "org.scalatest" %% "scalatest" % "3.2.0-M1" % Test,

  // local levelDB stores
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

  // JDBC with PostgresSQL
  "org.postgresql" % "postgresql" % postgresVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.2",

  // Cassandra
  "com.typesafe.akka" %% "akka-persistence-cassandra" % cassandraVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % cassandraVersion % Test
)
