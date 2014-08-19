package models.users.sorm

import play.api.Logger
import securesocial.core._
import org.joda.time.DateTime
import securesocial.core.IdentityId
import securesocial.core.providers.MailToken
import models.users.{TimeZone, UserProfile, UserDb}
import securesocial.core.services.UserService
import so.paws.db.DbPlugin
import com.typesafe.plugin._
import sorm.{Persisted, Instance}
import play.api.Play.current


object SormUserDb extends UserService {
  val db: Instance = use[DbPlugin[Instance]].db

  def find(identityId: IdentityId): Option[SocialUser] = {
    db.query[SocialUser]
      .whereEqual("identityId.userId", identityId.userId)
      .whereEqual("identityId.providerId", identityId.providerId)
      .fetchOne()
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[SocialUser] = {
    db.query[SocialUser]
      .whereEqual("email", Option(email))
      .whereEqual("identityId.providerId", providerId)
      .fetchOne()
  }

  def save(identity: Identity): SocialUser = {
    val socialUser: Option[SocialUser with Persisted] = db.query[SocialUser].whereEqual("email", identity.email).fetchOne()
    socialUser match {
      case None =>
        val savedIdentityId = db.save(identity.identityId)
        val newUser = SocialUser(
          savedIdentityId,
          identity.firstName,
          identity.lastName,
          identity.fullName,
          identity.email,
          identity.avatarUrl,
          db.save(identity.authMethod),
          identity.oAuth1Info.map(e => db.save(e)),
          identity.oAuth2Info.map(e => db.save(e)),
          identity.passwordInfo.map(e => db.save(e))
        )
        val newUserProfile = UserProfile(savedIdentityId, Set())
        db.transaction {
          db.save(newUser)
          db.save(newUserProfile)
        }
        newUser
      case Some(existingUser) =>
        val passInfo = Some(db.save(identity.passwordInfo.get))
        val updatedUser = existingUser.copy(identityId = identity.identityId, firstName = identity.firstName,
          lastName = identity.lastName, fullName = identity.fullName, email = identity.email, avatarUrl = identity.avatarUrl, authMethod = identity.authMethod,
          oAuth1Info = identity.oAuth1Info, oAuth2Info = identity.oAuth2Info, passwordInfo = passInfo)
        db.query[UserProfile].whereEqual("identityId", existingUser.identityId).fetchOne() match {
          case None => throw new IllegalStateException("Inconsistency between SocialUser and UserProfile")
          case Some(existingUserProfile) =>
            val updatedUserProfile = existingUserProfile.copy(identityId = identity.identityId, existingUserProfile.timezones)
            db.transaction {
              db.save(updatedUser)
              db.save(updatedUserProfile)
            }
        }
        updatedUser
    }
  }

  def save(token: Token) = {
    db.save(token)
  }

  def save(profile: UserProfile) = {
    db.save(profile)
  }

  def save(tz: TimeZone) = {
    db.save(tz)
  }

  def findToken(uuid: String): Option[Token] = {
    db.query[Token].whereEqual("uuid", uuid).fetchOne()
  }

  def deleteToken(uuid: String) {
    findToken(uuid).map(e => db.delete(e))
  }

  def deleteExpiredTokens() {
    db.query[Token].whereSmallerOrEqual("expirationTime", DateTime.now()).fetch() foreach { db.delete(_) }
  }

  def findAll:List[SocialUser] = {
    db.query[SocialUser].fetch().toList
  }

  def findEmailByTokenUuid(tokenUuid: String): Option[String] = {
    db.query[Token].whereEqual("uuid", tokenUuid).fetchOne().map(t => t.email)
  }

  override def findUserProfile(identityId: IdentityId): Option[UserProfile] = {
    db.query[UserProfile].whereEqual("identityId", identityId).fetchOne()
  }
}