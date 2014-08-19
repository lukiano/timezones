package models.users

import java.util.Locale

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}
import securesocial.core.{BasicProfile, Identity, SocialUser, IdentityId}

case class Authority(identityId: IdentityId, name: String)

case class TimeZone(uuid: String, name: String, city: String, zone: String) {
  import TimeZone._
  def timeZone: DateTimeZone = DateTimeZone.forID(zone)
  def timeInZoneAndGMT: (DateTime, DateTime) = {
    val now = DateTime.now
    (now.toDateTime(timeZone), now.toDateTime(utc))
  }
  def prettyTime: Array[String] = {
    val arr = new Array[String](2)
    val time = timeInZoneAndGMT
    arr(0) = formatter.print(time._1)
    arr(1) = formatter.print(time._2)
    arr
  }
}
object TimeZone {
  val formatter = DateTimeFormat.forPattern("dd-MMM-yy / HH:mm:ss").withLocale(Locale.US)
  val utc: DateTimeZone = DateTimeZone.UTC
}

case class UserProfile(identityId: UserProfile, timezones: Set[TimeZone])

abstract class UserDb {
  def find(identityId: UserProfile): Option[BasicProfile]

  def findByEmailAndProvider(email: String, providerId: String): Option[BasicProfile]

  def save(identity: Identity): BasicProfile

  def save(token: Token)

  def findToken(uuid: String): Option[Token]

  def deleteToken(uuid: String)

  def deleteExpiredTokens()

  def findAll: List[BasicProfile]

  def findEmailByTokenUuid(tokenUuid: String): Option[String]

  def findUserProfile(identityId: IdentityId): Option[UserProfile]

  def save(profile: UserProfile): UserProfile

  def save(tz: TimeZone): TimeZone
}
