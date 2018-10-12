package uk.ac.wellcome.platform.archive.registrar

import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{
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
    SNSClientModule,
    S3ClientModule,
    DynamoClientModule,
    MessageStreamModule
  )

  val app = injector.getInstance(classOf[Registrar])

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
