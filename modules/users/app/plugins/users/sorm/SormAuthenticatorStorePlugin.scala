package plugins.users.sorm

import play.api.Application
import plugins.users.AuthenticatorStorePlugin
import models.users.sorm.SormAuthenticatorDb

class SormAuthenticatorStorePlugin(app: Application) extends AuthenticatorStorePlugin(app) {
  val authenticatorDb = SormAuthenticatorDb
}
