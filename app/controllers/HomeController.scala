package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import services.{Show, Shows}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(shows: Shows)(implicit exec: ExecutionContext) extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action.async {
    shows.getAll().map { xs: List[Show] =>
      Ok(views.html.index(xs.flatMap({
        _ match {
          case Show(name, count) => List(s"{name: \'$name\', count: \'$count\'}")
          case _ => List.empty
        }
      }).mkString(",")))
    }
  }

  private def handleShowUpdate(updateMethod: String => Future[String]) = {
    Action.async {
      _.body.asFormUrlEncoded
        .flatMap(_.get("name"))
        .map(_.mkString(""))
      match {
        case Some(x) => updateMethod(x).map(Ok(_))
        case None => Future.apply(BadRequest("What did you do???"))
      }
    }
  }

  def increment = handleShowUpdate(shows.incrementShow)

  def decrement = handleShowUpdate(shows.decrementShow)

}
