package uk.ac.wellcome.platform.archive.common

import akka.Done
import grizzled.slf4j.Logging

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

trait WellcomeApp extends App with Logging {
  def run(): Future[Done]

  try {
    info("Starting worker.")

    val result = run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info("Terminating worker.")
  }
}
