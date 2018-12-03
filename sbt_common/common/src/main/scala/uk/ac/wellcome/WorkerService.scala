package uk.ac.wellcome

import akka.Done

import scala.concurrent.Future

trait WorkerService {
  def run(): Future[Done]
}
