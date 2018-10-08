package uk.ac.wellcome.platform.archive.progress_async

import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorClientModule
import uk.ac.wellcome.platform.archive.progress_http.modules._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with AkkaHttpApp {

  override val injector: Injector = Guice.createInjector(
    TypesafeConfigModule,
    CloudWatchClientModule,
    HttpServerConfigModule,
    MetricsConfigModule,
    ProgressMonitorClientModule,
    AkkaModule
  )

  try {
    info(s"Starting service.")

    val app = run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating service.")
  }
}
