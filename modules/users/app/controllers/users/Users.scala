package controllers.users

import _root_.java.util.UUID

import com.typesafe.plugin._
import org.joda.time.DateTimeZone
import play.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._
import play.api.templates.Txt
import securesocial.controllers.Registration._
import securesocial.controllers.TemplatesPlugin
import scala.io.Source
import scala.util.Random
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.users.{TimeZone, UserDb, UserProfile}
import securesocial.core._
import securesocial.core.OAuth1Info
import securesocial.core.IdentityId
import models.common.{Navigation, NavigationItem, NavigationMenu}

abstract class Users extends Controller with SecureSocial {
  val userDb: UserDb  // plugin appropriate implementation

  def navigation() = SecuredAction { implicit request =>
    val menus =
      Seq(
        NavigationMenu(
          Seq(
            NavigationItem("Change Password", "#/password/change")
          ),
          position = "left"
        ),
        NavigationMenu(
          Seq(
            NavigationItem("Sign Out", "#/signout")
          ),
          position = "right"
        )
      )

    val navigation = Navigation("default", menus)

    Ok(Navigation.toJson(navigation))
  }

  def generateName() = Action {
    implicit request =>
      Ok(Json.stringify(Json.obj(
        "firstName" -> getRandomName(1133, "/names/firstname.txt"),
        "lastName" -> getRandomName(979, "/names/lastname.txt")
      )))
  }

  def findEmailByTokenUuid(token: String) = Action {
    implicit request =>
      Ok(Json.stringify(Json.obj(
        "email" -> userDb.findEmailByTokenUuid(token)
      )))
  }

  def getRandomName(count: Int, path: String): String = {
    val nameIndex = Random.nextInt(count)
    var index = 0

    Source.fromInputStream(getClass.getResourceAsStream(path)).getLines().foreach(
      (name: String) => {
        if (nameIndex == index) return name.capitalize
        index = index + 1
      })
    ""
  }

  implicit val authenticationMethodFormat = Json.format[AuthenticationMethod]
  implicit val identityIdFormat = Json.format[IdentityId]
  implicit val oAuth1InfoFormat = Json.format[OAuth1Info]
  implicit val oAuth2InfoFormat = Json.format[OAuth2Info]
  implicit val passwordInfoFormat = Json.format[PasswordInfo]
  implicit val socialUserJsonFormat = (
    (__ \ 'identityId).format[IdentityId] and
      (__ \ 'firstName).format[String] and
      (__ \ 'lastName).format[String] and
      (__ \ 'fullName).format[String] and
      (__ \ 'email).formatNullable[String] and
      (__ \ 'avatarUrl).formatNullable[String] and
      (__ \ 'authenticationMethod).format[AuthenticationMethod] and
      (__ \ 'oAuth1Info).formatNullable[OAuth1Info] and
      (__ \ 'oAuth2Info).formatNullable[OAuth2Info] and
      (__ \ 'passwordInfo).formatNullable[PasswordInfo]
    )(SocialUser.apply(
    _: IdentityId,
    _: String,
    _: String,
    _: String,
    _: Option[String],
    _: Option[String],
    _: AuthenticationMethod,
    _: Option[OAuth1Info],
    _: Option[OAuth2Info],
    _: Option[PasswordInfo]
  ), unlift(SocialUser.unapply))
  implicit val timeZoneFormat = Json.format[TimeZone]
  implicit val userProfileFormat = Json.format[UserProfile]

  def getUsers = SecuredAction {
    implicit request => {
      val allUsers: List[SocialUser] = userDb.findAll
      Ok(Json.toJson(allUsers))
    }
  }

  def home = SecuredAction {
    implicit request => {
      val profile = for {
        authenticator <- SecureSocial.authenticatorFromRequest
        user <- userDb.findUserProfile(authenticator.identityId)
      } yield user
      profile.fold[SimpleResult](Forbidden("")) { p: UserProfile =>
        Logger.info(p.toString)
        Ok(views.html.users.home(p))
      }
    }
  }

  val validCities: Set[String] = {
    import scala.collection.JavaConverters._
    DateTimeZone.getProvider.getAvailableIDs.asScala.toSet.map{ s: String => s.toLowerCase }
  }

  val validCity: String => Boolean = city => {
    val l = city.trim.toLowerCase.replace(' ', '_')
    validCities exists { _ endsWith l }
  }

  case class NewTimeZone(name: String, city: String)

  val newTimeZoneForm = Form[NewTimeZone](
    mapping(
      "name" -> nonEmptyText(),
      "city" -> nonEmptyText().verifying(validCity)
    )
    (NewTimeZone.apply)
    (NewTimeZone.unapply)
  )

  def addTimeZone = SecuredAction {
    implicit request =>
      newTimeZoneForm.bindFromRequest.fold (
        errors => {
          BadRequest(Txt(""))
        },
        ntz => {
          val profile = for {
            authenticator <- SecureSocial.authenticatorFromRequest
            user <- userDb.findUserProfile(authenticator.identityId)
          } yield user
          profile.fold[SimpleResult](Forbidden("")) { p: UserProfile =>
            val cityInLowercase = ntz.city.trim.toLowerCase.replace(' ', '_')
            validCities find { city => city endsWith cityInLowercase } match {
              case None => BadRequest(Txt(""))
              case Some(timeZoneOfCity) =>
                val tz = TimeZone(UUID.randomUUID().toString, ntz.name, ntz.city, timeZoneOfCity)
                val persistedTz: TimeZone = userDb.save(tz)
                val updatedProfile = p.copy(timezones = p.timezones + persistedTz)
                userDb.save(updatedProfile)
                Ok(Txt(""))
            }
          }
        }
      )

  }
}
