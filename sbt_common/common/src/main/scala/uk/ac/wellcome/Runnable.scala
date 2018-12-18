package uk.ac.wellcome

import scala.concurrent.Future

trait Runnable {
  def run(): Future[Any]
}
