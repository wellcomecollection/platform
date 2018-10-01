package uk.ac.wellcome.platform.archive.call_backerei

import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorModule
import uk.ac.wellcome.platform.archive.call_backerei.modules.{
  AppConfigModule,
  ConfigModule,
  VHSModule
}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
    VHSModule,
    AkkaModule,
    CloudWatchClientModule,
    SQSClientModule,
    SNSAsyncClientModule,
    DynamoClientModule,
    ProgressMonitorModule,
    MessageStreamModule
  )

  val app = injector.getInstance(classOf[CallBäckerei])

  try {
    info(s"Starting worker.")

    val result = app.run()

    Await.result(result, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
