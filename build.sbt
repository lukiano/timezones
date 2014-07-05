import sbt._

organization := "com.toptal.tzv"

name := "timezoneviewer"

version := "1.0-SNAPSHOT"

play.Project.playScalaSettings

lazy val db = project.in(file("modules/db"))

lazy val common = project.in(file("modules/common"))
  .dependsOn(db).aggregate(db)

lazy val users = project.in(file("modules/users"))
  .dependsOn(common, db).aggregate(common, db)

lazy val root = project.in(file("."))
  .dependsOn(common, db, users)
  .aggregate(common, db, users)

resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk/"


libraryDependencies ++= Seq(
  "com.newrelic.agent.java" % "newrelic-agent" % "3.8.0",
  "com.newrelic.agent.java" % "newrelic-api" % "3.8.0",
  "com.typesafe" %% "play-plugins-redis" % "2.2.1"
)