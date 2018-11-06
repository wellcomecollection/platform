package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.google.inject.{AbstractModule, Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.modules._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object Main extends App {
  val app = new NotifierApp()
  app.run()
}

class NotifierApp() extends WellcomeApp {
  val configModule = TypesafeConfigModule

  val modules: List[AbstractModule] = List(
    AkkaModule,
    CloudWatchModule,
    MetricsModule,
    SNSModule,
    SQSModule
  )

  val injector: Injector =
    Guice.createInjector(configModule :: modules : _*)

  def run(): Future[Done] = {
    val classToGet = classOf[SQSConfig]
    val sqsConfig = injector.getInstance(classToGet)
    val sqsClient = injector.getInstance(classOf[AmazonSQSAsync])
    val snsClient = injector.getInstance(classOf[AmazonSNS])
    val snsConfig = injector.getInstance(classOf[SNSConfig])
    val metricsSender = injector.getInstance(classOf[MetricsSender])
    val notifier = new Notifier(
      sqsClient = sqsClient,
      sqsConfig = sqsConfig,
      snsClient = snsClient,
      snsConfig = snsConfig,
      metricsSender = metricsSender,
      contextUrl = new URL("https://example.org")
    )(
      actorSystem = injector.getInstance(classOf[ActorSystem]),
      materializer = injector.getInstance(classOf[ActorMaterializer])
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
      System.exit(1)
  } finally {
    info("Terminating service.")
    System.exit(0)
  }
}
