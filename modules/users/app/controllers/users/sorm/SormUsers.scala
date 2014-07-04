package controllers.users.sorm

import models.users.sorm.SormUserDb
import controllers.users.Users

object SormUsers extends Users {
  val userDb = SormUserDb
}
