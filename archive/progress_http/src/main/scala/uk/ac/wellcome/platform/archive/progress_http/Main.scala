package uk.ac.wellcome.platform.archive.progress_async

import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.progress_http.ProgressHttp
import uk.ac.wellcome.platform.archive.progress_http.modules.{AppConfigModule, ConfigModule}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with ProgressHttp {
  override val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
    AkkaModule,
    ProgressMonitorModule
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
