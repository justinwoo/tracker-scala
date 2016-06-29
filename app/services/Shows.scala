package services

import java.io.File
import javax.inject._

import com.redis.RedisClient

import scala.concurrent.{Future, Promise}
import scala.util.matching.Regex

case class Show(name: String, count: String)

/**
 * This trait demonstrates how to create a component that is injected
 * into a controller. The trait represents a counter that returns a
 * incremented number each time it is called.
 */
trait Shows {
  def getAll(): Future[List[Show]]
  def incrementShow(name: String): Future[String]
  def decrementShow(name: String): Future[String]
}

/**
 * This class has a `Singleton` annotation because we need to make
 * sure we only use one counter per application. Without this
 * annotation we would get a new instance every time a [[Shows]] is
 * injected.
 */
@Singleton
class AtomicShows() extends Shows {
  private val r = new RedisClient("localhost", 6379)
  private val names: List[String] = new File(sys.env("ANIMU_HOME")).listFiles().flatMap(f => {
    val (filename, head, tail) = (f.getName, "^\\[.*\\] ".r, " - \\d*.*$".r)
    (head findFirstIn filename, tail findFirstIn filename) match {
      case (Some(_), Some(_)) => {
        def remove(r: Regex): (String) => String = r.replaceFirstIn (_, "")
        def strip = remove(head) compose remove(tail)
        List(strip(filename))
      }
      case (_, _) => List.empty
    }
  }).toList

  for (name <- names) {
    r.setnx(name, "0")
  }

  private def getShow(name: String): Show = {
    val count = r.get(name) match {
      case Some(x) => x.toString
      case None => "error"
    }
    Show(name, count)
  }

  override def getAll(): Future[List[Show]] = {
    val promise: Promise[List[Show]] = Promise[List[Show]]()
    val shows: List[Show] = names.map(getShow)
    promise.success(shows)
    promise.future
  }

  private def updateShow(name: String, method: (String) => Option[Long]): String = {
    method(name) match {
      case Some(x) => x.toString
      case None => "error"
    }
  }

  override def incrementShow(name: String): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    promise.success(updateShow(name, r.incr))
    promise.future
  }

  override def decrementShow(name: String): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    promise.success(updateShow(name, r.decr))
    promise.future
  }
}
