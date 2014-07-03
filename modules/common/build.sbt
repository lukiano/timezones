organization := "so.paws"

name := "common"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "webjars-locator" % "0.12",
  "org.webjars" % "angularjs" % "1.2.13",
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "requirejs" % "2.1.10"
)

play.Project.playScalaSettings
