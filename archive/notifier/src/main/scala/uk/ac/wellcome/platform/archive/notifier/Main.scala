package uk.ac.wellcome.platform.archive.notifier

import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.notifier.modules.{
  AppConfigModule,
  ConfigModule
}
import uk.ac.wellcome.platform.archive.common.modules.{
  AkkaModule,
  CloudWatchClientModule,
  SNSClientModule,
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
    SQSClientModule
  )

  val app = injector.getInstance(classOf[Notifier])

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
