package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.modules._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object Main extends App {
  val app = new NotifierApp()
  app.run()
}

class NotifierApp() extends WellcomeApp {

  def run(): Future[Done] = {
    val config = ConfigFactory.load()

    val sqsClientConfig = SQSModule.providesSQSClientConfig(config)
    val sqsClient = SQSModule.providesSQSAsyncClient(sqsClientConfig)
    val sqsConfig = SQSModule.providesSQSConfig(config)

    val snsConfig = SNSModule.providesSNSConfig(config)
    val snsClientConfig = SNSModule.providesSNSClientConfig(config)
    val snsClient = SNSModule.providesSNSClient(snsClientConfig)

    val cloudwatchClientConfig =
      CloudWatchModule.providesCloudWatchClientConfig(config)
    val cloudWatchClient: AmazonCloudWatch =
      CloudWatchModule.providesAmazonCloudWatch(cloudwatchClientConfig)

    implicit val actorSystem: ActorSystem = ActorSystem("main-actor-system")
    val actorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem))

    val metricsConfig = MetricsModule.providesMetricsConfig(config)
    val metricsSender = MetricsModule.providesMetricsSender(
      amazonCloudWatch = cloudWatchClient,
      actorSystem = actorSystem,
      metricsConfig = metricsConfig
    )

    val notifier = new Notifier(
      sqsClient = sqsClient,
      sqsConfig = sqsConfig,
      snsClient = snsClient,
      snsConfig = snsConfig,
      metricsSender = metricsSender,
      contextUrl = new URL("https://example.org")
    )(
      actorSystem = actorSystem,
      materializer = actorMaterializer
    )

    notifier.run()
  }
}

trait WellcomeApp extends Logging {
  def run(): Future[Done]

  try {
    info("Starting service.")
    val app = run()
    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
//      System.exit(1)
  } finally {
    info("Terminating service.")
//    System.exit(0)
  }
}
