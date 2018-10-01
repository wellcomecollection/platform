package uk.ac.wellcome.platform.archive.call_backerei

import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.call_backerei.modules.{
  AppConfigModule,
  ConfigModule
}
import uk.ac.wellcome.platform.archive.common.modules.{
  AkkaModule,
  CloudWatchClientModule,
  SNSAsyncClientModule,
  SQSClientModule
}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with Logging {
  val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
    AkkaModule,
    CloudWatchClientModule,
    SQSClientModule,
    SNSAsyncClientModule
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
