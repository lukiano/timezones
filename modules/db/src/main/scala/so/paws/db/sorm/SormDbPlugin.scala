package so.paws.db.sorm

import java.net.URI

import org.joda.time.DateTimeZone
import play.api.{Logger, Play, Application}
import sorm.{Entity, Instance, InitMode}
import scala.reflect.runtime.universe
import so.paws.db.DbPlugin
import scala.collection.JavaConversions._

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
/*
        Logger(classOf[SormDbPlugin]).info(url)
        Logger(classOf[SormDbPlugin]).info(dbUri.getHost)
        Logger(classOf[SormDbPlugin]).info(Integer.toString(dbUri.getPort))
        Logger(classOf[SormDbPlugin]).info(dbUri.getUserInfo)
        Logger(classOf[SormDbPlugin]).info(urlString)
*/
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