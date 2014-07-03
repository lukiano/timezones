package models.common.sorm

import so.paws.db.sorm.SormEntities
import sorm.Entity
import models.common.{NavigationItem, NavigationMenu, Navigation}

/**
 * Created by jd on 2/5/14.
 */
object CommonSormEntities extends SormEntities {
  override def get: Set[Entity] = {
    Set(
      Entity[Navigation](unique = Set() + Seq("page")),
      Entity[NavigationMenu](),
      Entity[NavigationItem]()
    )
  }
}
