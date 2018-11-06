package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
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

  def run(): Future[Done] = {
    val notifier = new Notifier(
      sqsClient = injector.getInstance(classOf[AmazonSQSAsyncClient]),
      sqsConfig = injector.getInstance(classOf[SQSConfig]),
      snsClient = injector.getInstance(classOf[AmazonSNS]),
      snsConfig = injector.getInstance(classOf[SNSConfig]),
      metricsSender = injector.getInstance(classOf[MetricsSender]),
      contextUrl = new URL("https://example.org")
    )(
      actorSystem = injector.getInstance(classOf[ActorSystem]),
      materializer = injector.getInstance(classOf[ActorMaterializer])
    )

    notifier.run()
  }
}

trait WellcomeApp extends Logging {
  val configModule: Configurable
  val modules: List[AbstractModule]

  def injector: Injector =
    Guice.createInjector(configModule :: modules : _*)

  def run(): Future[Done]

  try {
    info("Starting service.")
    val app = run()
    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info("Terminating service.")
  }
}
