package models.users.sorm

import com.lucho.tzv.db.sorm.SormEntities
import models.users.{TimeZone, UserProfile, Authority}
import plugins.users.AuthenticatorEntity
import securesocial.core._
import securesocial.core.IdentityId
import securesocial.core.OAuth1Info
import securesocial.core.OAuth2Info
import securesocial.core.PasswordInfo
import securesocial.core.providers.Token
import sorm.Entity

object UserSormEntities extends SormEntities {
  def get: Set[Entity] = {
    Set(
      Entity[Authority](unique = Set() + Seq("identityId", "name")),
      Entity[AuthenticatorEntity](),
      Entity[TimeZone](unique = Set() + Seq("uuid")),
      Entity[UserProfile](unique = Set() + Seq("identityId")),
      /**
       * Secure Social Entities
       */
      Entity[AuthenticationMethod](indexed = Set() + Seq("method")),
      Entity[IdentityId](
        indexed = Set() + Seq("providerId", "userId"),
        unique = Set() + Seq("providerId", "userId")
      ),
      Entity[OAuth1Info](),
      Entity[OAuth2Info](),
      Entity[PasswordInfo](indexed = Set() + Seq("password")),
      Entity[SocialUser](indexed = Set() + Seq("email")),
      Entity[Token](indexed = Set() + Seq("uuid") + Seq("email"))
    )
  }
}