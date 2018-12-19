package uk.ac.wellcome

import grizzled.slf4j.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait WellcomeApp extends App with Logging {
  def run(workerService: Runnable) =
    try {
      info(s"Starting worker.")

      val result = workerService.run()

      Await.result(result, Duration.Inf)
    } catch {
      case e: Throwable =>
        error("Fatal error:", e)
    } finally {
      info(s"Terminating worker.")
    }
}
