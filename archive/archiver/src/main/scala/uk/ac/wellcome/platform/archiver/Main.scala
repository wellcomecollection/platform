package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject._
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.utils.JsonUtil._


object Main extends App with Logging{
  lazy val injector = Guice.createInjector(
    new AppConfigModule(args),
    AkkaModule,
    AkkaS3ClientModule,
    MessageStreamModule,
    CloudWatchClientModule,
    SQSClientModule,
  )

  val actorSystem = injector.getInstance(classOf[ActorSystem])

  try {
    debug(s"Starting worker.")

    val archiver = injector.getInstance(classOf[Archiver])
    archiver.run

  } finally {
    debug(s"Terminating worker.")
    actorSystem.terminate
  }
}

class Archiver(messageStream: MessageStream[NotificationMessage, Unit]) extends Logging {
  def run = {
    val workFlow = Flow[NotificationMessage].map(println)
    messageStream.run("archiver", workFlow)
  }
}

object MessageStreamModule extends AbstractModule {

  @Provides
  def providesMessageStream(actorSystem: ActorSystem,
                            sqsClient: AmazonSQSAsync,
                            sqsConfig: SQSConfig,
                            metricsSender: MetricsSender) = {

    new MessageStream[NotificationMessage, Unit](
      actorSystem, sqsClient, sqsConfig, metricsSender
    )
  }

}