package models.common.sorm

import so.paws.db.sorm.SormEntities
import sorm.Entity
import models.common.{NavigationItem, NavigationMenu, Navigation}

object CommonSormEntities extends SormEntities {
  override def get: Set[Entity] = {
    Set(
      Entity[Navigation](unique = Set() + Seq("page")),
      Entity[NavigationMenu](),
      Entity[NavigationItem]()
    )
  }
}
