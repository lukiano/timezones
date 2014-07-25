import sbt.Keys._
import sbt._
import com.typesafe.sbt.web.SbtWeb
import SbtWeb.autoImport.pipelineStages
import com.typesafe.sbt.rjs.SbtRjs
import SbtRjs.autoImport.rjs
import com.typesafe.sbt.digest.SbtDigest
import SbtDigest.autoImport.digest
import com.typesafe.sbt.gzip.SbtGzip
import SbtGzip.autoImport.gzip

object TZWBuild extends Build {

  lazy val commonSettings = Seq[Setting[_]](
    organization := "com.lucho.tzv",
    version := "2.0-SNAPSHOT",
    scalaVersion := "2.10.4"
  )

  lazy val db = Project(id = "db", base = file("modules/db"), settings = commonSettings).enablePlugins(play.PlayScala)
    .settings {
    libraryDependencies ++= Seq(
      "com.h2database" % "h2" % "1.3.174",
      "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
      "org.sorm-framework" % "sorm" % "0.3.12"
    )
  }

  lazy val common = Project(id = "common", base = file("modules/common"), settings = commonSettings).enablePlugins(play.PlayScala)
    .dependsOn(db).aggregate(db).settings {
    libraryDependencies ++= Seq(
      "org.webjars" %% "webjars-play" % "2.3.0",
      "org.webjars" % "webjars-locator" % "0.17",
      "org.webjars" % "angularjs" % "1.2.20",
      "org.webjars" % "bootstrap" % "3.2.0",
      "org.webjars" % "requirejs" % "2.1.14"
    )
  }

  lazy val users = Project(id = "users", base = file("modules/users"), settings = commonSettings).enablePlugins(play.PlayScala)
    .dependsOn(common, db).aggregate(common, db).settings(
      resolvers ++= Seq(
        Resolver.sonatypeRepo("snapshots")
      ),
      libraryDependencies ++= Seq(
        "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"
      )
    )

  lazy val root = Project(id = "root", base = file("."), settings = commonSettings).enablePlugins(play.PlayScala).enablePlugins(SbtWeb)
    .settings(
      name := "timezoneviewer",
      pipelineStages := Seq(rjs, digest, gzip),
      resolvers ++= Seq(
        "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk/",
        Resolver.sonatypeRepo("snapshots")
      ),
      libraryDependencies ++= Seq(
        "com.newrelic.agent.java" % "newrelic-agent" % "3.8.0",
        "com.newrelic.agent.java" % "newrelic-api" % "3.8.0",
        "com.typesafe.play.plugins" %% "play-plugins-redis" % "2.3.0"
      )
    ).aggregate(common, db, users)

}