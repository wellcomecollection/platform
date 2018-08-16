package uk.ac.wellcome.platform.archive.common.app

import com.google.inject.{AbstractModule, Injector}
import grizzled.slf4j.Logging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait WellcomeApp[W <: Worker[R], R] extends App with InjectedModules {
  val worker: W

  try {
    info(s"Starting worker.")
    val app = worker.run()
    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}

trait InjectedModules extends Logging {
  val configuredModules: List[AbstractModule]
  val appConfigModule: AbstractModule

  val modules: List[AbstractModule] = appConfigModule :: configuredModules
  val injector: Injector
}

trait Worker[T <: Any] {
  def run(): Future[T]
}
