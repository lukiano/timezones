package com.lucho.tzv.db.sorm

import java.net.URI

import com.lucho.tzv.db.DbPlugin
import play.api.{Application, Play}
import sorm.{Entity, InitMode, Instance}

import scala.collection.JavaConversions._
import scala.reflect.runtime.universe

class SormDbPlugin(application: Application) extends DbPlugin[Instance] {

  override def db: Instance = {
    var entities: Set[Entity] = Set[Entity]()

    application.configuration.getStringList("sorm.entities").get.foreach ( e =>
      entities ++= getEntities(application, e)
    )
    Play.current.configuration.getString("sorm.url") match {
      case Some(url) =>
        val dbUri = new URI(url)
        val port = if (dbUri.getPort == -1) "" else ":" + dbUri.getPort
        val urlString = "jdbc:postgresql://"+ dbUri.getHost + port + dbUri.getPath
        new Instance(
          entities = entities,
          url = urlString,
          user = dbUri.getUserInfo.split(":")(0),
          password = dbUri.getUserInfo.split(":")(1),
          initMode = InitMode.Create,
          poolSize = 64
        )
      case None =>
        throw new Exception("sorm.url not set!")
    }
  }

  def getEntities(application: Application, ref: String): Set[Entity] = {
    val runtimeMirror = universe.runtimeMirror(application.classloader)

    val module = runtimeMirror.staticModule(ref)

    val obj = runtimeMirror.reflectModule(module)

    obj.instance match {
      case sormEntities: SormEntities => sormEntities.get
      case _ => throw new ClassCastException
    }
  }
}