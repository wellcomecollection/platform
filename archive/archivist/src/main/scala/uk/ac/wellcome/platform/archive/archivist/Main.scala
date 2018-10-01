package uk.ac.wellcome.platform.archive.archivist

import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.platform.archive.archivist.modules.{
  AppConfigModule,
  ConfigModule
}
import uk.ac.wellcome.platform.archive.common.modules._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with Archivist {
  override val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
    AkkaModule,
    S3ClientModule,
    CloudWatchClientModule,
    SQSClientModule,
    SNSAsyncClientModule,
    MessageStreamModule
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
