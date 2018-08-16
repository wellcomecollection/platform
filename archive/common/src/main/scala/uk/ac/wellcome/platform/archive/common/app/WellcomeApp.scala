package uk.ac.wellcome.platform.archive.common.app

import com.google.inject.{AbstractModule, Guice, Injector}
import grizzled.slf4j.Logging
import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

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

  lazy val modules: List[AbstractModule] = appConfigModule :: configuredModules
  lazy val injector: Injector = Guice.createInjector(modules.asJava)
}

trait Worker[T <: Any] {
  def run(): Future[T]
}
