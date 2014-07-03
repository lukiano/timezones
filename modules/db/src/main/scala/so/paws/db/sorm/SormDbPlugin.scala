package so.paws.db.sorm

import java.net.URI

import play.api.Play
import sorm.{Entity, Instance, InitMode}
import scala.reflect.runtime.universe
import so.paws.db.DbPlugin
import play.api.Application
import scala.collection.JavaConversions._

class SormDbPlugin(application: Application) extends DbPlugin[Instance] {

  override def db: Instance = {
    var entities: Set[Entity] = Set[Entity]()

    application.configuration.getStringList("sorm.entities").get.foreach ( e =>
      entities ++= getEntities(application, e)
    )
    val dbUri = new URI(System.getenv("DATABASE_URL"))
    val username = dbUri.getUserInfo().split(":")(0)
    val password = dbUri.getUserInfo().split(":")(1)
    new Instance(
      entities = entities,
      url = "jdbc:postgresql://"+ dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath(),
    //Play.current.configuration.getString("db.default.url").getOrElse(""), // "jdbc:h2:./db/paws;AUTO_SERVER=TRUE",
      user = username,
      password = password,
      initMode = InitMode.DropAllCreate,
      poolSize = 100
    )
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