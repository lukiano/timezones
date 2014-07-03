organization := "so.paws"

name := "db"

version := "1.0-SNAPSHOT"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  jdbc,
  "com.h2database" % "h2" % "1.3.174",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "com.typesafe.play" %% "play" % "2.2.2",
  "org.sorm-framework" % "sorm" % "0.3.12",
  "com.typesafe.slick" %% "slick" % "2.0.0"
)
