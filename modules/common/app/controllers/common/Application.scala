package controllers.common

import play.api.mvc.{Action, Controller}
import models.common.{NavigationItem, NavigationMenu, Navigation}
import play.api.templates.Html
import play.api.http.MimeTypes


object Application extends Controller {
  def navigation() = Action { implicit request =>
    val menus =
      Seq(
        NavigationMenu(
          Seq(
            NavigationItem("Sign Up", "#/signup"),
            NavigationItem("Sign In", "#/login")
          ),
          position = "left"
        )
      )

    val navigation = Navigation("default", menus)

    Ok(Navigation.toJson(navigation))
  }

  def setupRequireJs = Action {
    Ok(org.webjars.RequireJS.getSetupJavaScript("/webjars/")).as(MimeTypes.JAVASCRIPT)
  }
}
