package uk.ac.wellcome.platform.archive.registrar

import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.platform.archive.registrar.modules.{
  AppConfigModule,
  ConfigModule
}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with Registrar {
  override val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
  )

  try {
    info(s"Starting worker.")

    val app = run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
