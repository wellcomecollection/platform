package uk.ac.wellcome.platform.archiver

import akka.stream.scaladsl.Flow
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Await
import scala.concurrent.duration._


object Main extends ArchiverApp {
  override val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    AkkaModule,
    AkkaS3ClientModule,
    CloudWatchClientModule,
    SQSClientModule,
    MessageStreamModule
  )

  run()
}



trait ArchiverApp extends App with Logging {
  val injector: Injector

  def run() = {
    try {
      info(s"Starting worker.")

      val messageStream = injector.getInstance(classOf[MessageStream[NotificationMessage, Unit]])
      val workFlow = Flow[NotificationMessage].map(println)

      val done = messageStream.run("archiver", workFlow)
      Await.result(done, Duration.Inf)

    } finally {
      info(s"Terminating worker.")
    }
  }
}