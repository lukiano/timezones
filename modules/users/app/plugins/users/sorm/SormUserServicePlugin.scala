package plugins.users.sorm

import plugins.users.UserServicePlugin

import play.api.Application
import models.users.sorm.SormUserDb

class SormUserServicePlugin(application: Application) extends UserServicePlugin(application) {
  val userDb = SormUserDb
}
