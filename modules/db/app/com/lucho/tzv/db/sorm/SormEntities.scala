package com.lucho.tzv.db.sorm

import sorm.Entity

trait SormEntities {
  def get: Set[Entity]
}