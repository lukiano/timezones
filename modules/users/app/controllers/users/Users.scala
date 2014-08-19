package controllers.users

import _root_.java.util.UUID

import org.joda.time.DateTimeZone
import play.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.twirl.api.Txt
import securesocial.core.providers.UsernamePasswordProvider
import scala.io.Source
import scala.util.{Try, Random}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.users.{TimeZone, UserDb, UserProfile}
import securesocial.core._
import securesocial.core.OAuth1Info
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
  implicit val identityIdFormat = Json.format[UserProfile]
  implicit val oAuth1InfoFormat = Json.format[OAuth1Info]
  implicit val oAuth2InfoFormat = Json.format[OAuth2Info]
  implicit val passwordInfoFormat = Json.format[PasswordInfo]
  implicit val socialUserJsonFormat = (
    (__ \ 'providerId).format[String] and
      (__ \ 'userId).format[String] and
      (__ \ 'firstName).formatNullable[String] and
      (__ \ 'lastName).formatNullable[String] and
      (__ \ 'fullName).formatNullable[String] and
      (__ \ 'email).formatNullable[String] and
      (__ \ 'avatarUrl).formatNullable[String] and
      (__ \ 'authenticationMethod).format[AuthenticationMethod] and
      (__ \ 'oAuth1Info).formatNullable[OAuth1Info] and
      (__ \ 'oAuth2Info).formatNullable[OAuth2Info] and
      (__ \ 'passwordInfo).formatNullable[PasswordInfo]
    )(BasicProfile.apply(
    _: String,
    _: String,
    _: Option[String],
    _: Option[String],
    _: Option[String],
    _: Option[String],
    _: Option[String],
    _: AuthenticationMethod,
    _: Option[OAuth1Info],
    _: Option[OAuth2Info],
    _: Option[PasswordInfo]
  ), unlift(BasicProfile.unapply))
  implicit val timeZoneFormat = Json.format[TimeZone]
  implicit val userProfileFormat = Json.format[UserProfile]

  def getUsers = SecuredAction {
    implicit request => {
      val allUsers: List[BasicProfile] = userDb.findAll
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
        Ok(views.html.users.home(p))
      }
    }
  }

  case class TimeZoneDTO(name: String, city: String, time: String)

  sealed trait SortingType {
    def ordering: Ordering[String]
  }
  case object Asc extends SortingType {
    val ordering = Ordering[String]
  }
  case object Desc extends SortingType {
    val ordering = Ordering[String].reverse
  }
  private val toSorting: String => Option[SortingType] = _.trim.toLowerCase match {
    case "asc" => Some(Asc)
    case "desc" => Some(Desc)
    case _ => None
  }
  case class Sorting(ex: Extractor, sortingType: SortingType)
  private val sortingProcess: Sorting => Process = s =>
    data => data.sortBy[String](s.ex)(s.sortingType.ordering)

  type Extractor = TimeZone => String
  private val nameExtractor: Extractor = tz => tz.name.toLowerCase
  private val cityExtractor: Extractor = tz => tz.city.toLowerCase

  case class Filter(ex: Extractor, filtering: String)
  private val filterProcess: Filter => Process = f =>
    data => data filter { tz => f.ex(tz).startsWith(f.filtering) }

  private val toInt: String => Option[Int] = s => Try(s.toInt).toOption

  type Data = Seq[TimeZone]
  type Process = Data => Data
  private val identity: Process = d => d
  implicit final class OptionProcess[A](op: Option[A]) {
    def toProcess(f: A => Process): Process = op match {
      case None => identity
      case Some(a) => f(a)
    }
  }

  case class Page(page: Int, elementsPerPage: Int)

  def homeJson = SecuredAction {
    implicit request => {
      val profile = for {
        authenticator <- SecureSocial.authenticatorFromRequest
        user <- userDb.findUserProfile(authenticator.identityId)
      } yield user
      profile.fold[SimpleResult](Forbidden("")) { p: UserProfile =>
        val paging = (for {
          c <- request.getQueryString("count") flatMap toInt
          p <- request.getQueryString("page") flatMap toInt filter { _ > 1 }
        } yield Page(p, c)) toProcess { page =>
          data => data.drop((page.page - 1) * page.elementsPerPage).take(page.elementsPerPage)
        }

        val filterName = request.getQueryString("filter[name]") map { prefix =>
          Filter(nameExtractor, prefix.toLowerCase) } toProcess filterProcess
        val filterCity = request.getQueryString("filter[city]") map { prefix =>
          Filter(cityExtractor, prefix.toLowerCase) } toProcess filterProcess

        val sortingName = request.getQueryString("sorting[name]") flatMap toSorting map { sortingType =>
          Sorting(nameExtractor, sortingType) } toProcess sortingProcess
        val sortingCity = request.getQueryString("sorting[city]") flatMap toSorting map { sortingType =>
          Sorting(cityExtractor, sortingType) } toProcess sortingProcess

        val filteredValues: Data = p.timezones.toSeq
        val process = paging andThen filterName andThen filterCity andThen sortingName andThen sortingCity

        val seqOfTimeZones: Seq[JsObject] = process(filteredValues) map { tz =>
          Json.obj(
            "name" -> tz.name,
            "city" -> tz.city,
            "time" -> tz.prettyTime(0)
          )
        }
        val dto = Json.obj(
          "total" -> seqOfTimeZones.size,
          "result" -> JsArray(seqOfTimeZones)
        )
        Ok(dto)
      }
    }
  }

  val validCities: Set[String] = {
    import scala.collection.JavaConverters._
    DateTimeZone.getProvider.getAvailableIDs.asScala.toSet
  }

  val validCity: String => Boolean = s => {
    val l = s.trim.toLowerCase.replace(' ', '_')
    Logger.debug(s"Validating city $l")
    validCities exists { city => city.toLowerCase endsWith l }
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
            validCities find { city => city.toLowerCase endsWith cityInLowercase } match {
              case None => BadRequest(Txt(cityInLowercase))
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
  /* REST API */
  def restTimeZones = Action {
    implicit request =>
      val timezones: Option[Set[TimeZone]] = for {
        email <- request.headers.get("TZ_USER")
        pass <- request.headers.get("TZ_PASS")
        socialUser <- userDb.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword)
        savedPass <- socialUser.passwordInfo if Registry.hashers.currentHasher.matches(savedPass, pass)
        userProfile <- userDb.findUserProfile(socialUser.identityId)
      } yield userProfile.timezones
      timezones.fold[SimpleResult](Forbidden("")) { tz: Set[TimeZone] => Ok(Json.toJson(tz)) }
  }

  def restTimeZone(zoneName: String) = Action {
    implicit request =>
      val timezones: Option[Set[TimeZone]] = for {
        email <- request.headers.get("TZ_USER")
        pass <- request.headers.get("TZ_PASS")
        socialUser <- userDb.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword)
        savedPass <- socialUser.passwordInfo if Registry.hashers.currentHasher.matches(savedPass, pass)
        userProfile <- userDb.findUserProfile(socialUser.identityId)
      } yield userProfile.timezones
      timezones.fold[SimpleResult] (Forbidden("")) { tz: Set[TimeZone] =>
        tz.find(_.name == zoneName) match {
          case None => NotFound("")
          case Some(value) => Ok(Json.toJson(value))
        }
      }
  }

  def restAddTimeZone() = Action(parse.json) {
    implicit request =>
      val userProfile: Option[UserProfile] = for {
        email <- request.headers.get("TZ_USER")
        pass <- request.headers.get("TZ_PASS")
        socialUser <- userDb.findByEmailAndProvider(email, UsernamePasswordProvider.UsernamePassword)
        savedPass <- socialUser.passwordInfo if Registry.hashers.currentHasher.matches(savedPass, pass)
        userProfile <- userDb.findUserProfile(socialUser.identityId)
      } yield userProfile
      userProfile.fold[SimpleResult] (Forbidden("")) { p: UserProfile =>
        newTimeZoneForm.bind(request.body).fold (
          errors => {
            BadRequest(Txt(""))
          },
          ntz => {
            val cityInLowercase = ntz.city.trim.toLowerCase.replace(' ', '_')
            validCities find { city => city.toLowerCase endsWith cityInLowercase } match {
              case None => BadRequest(Txt(cityInLowercase))
              case Some(timeZoneOfCity) =>
                val tz = TimeZone(UUID.randomUUID().toString, ntz.name, ntz.city, timeZoneOfCity)
                val persistedTz: TimeZone = userDb.save(tz)
                val updatedProfile = p.copy(timezones = p.timezones + persistedTz)
                userDb.save(updatedProfile)
                Created(Txt("")).withHeaders("Location" -> s"/users/rest/timezone/${tz.name}")
            }
          })
      }
  }
}
