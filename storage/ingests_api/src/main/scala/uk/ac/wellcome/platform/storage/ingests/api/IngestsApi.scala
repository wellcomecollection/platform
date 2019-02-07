package uk.ac.wellcome.platform.storage.ingests.api

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import grizzled.slf4j.Logging
import uk.ac.wellcome.Runnable
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.http.{HttpMetrics, WellcomeHttpApp}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}

class IngestsApi(
  dynamoClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig,
  snsWriter: SNSWriter,
  httpMetrics: HttpMetrics,
  httpServerConfig: HTTPServerConfig,
  contextURL: URL
)(implicit val actorSystem: ActorSystem,
  materializer: ActorMaterializer,
  executionContext: ExecutionContext)
    extends Logging
    with Runnable {
  val progressTracker = new ProgressTracker(
    dynamoDbClient = dynamoClient,
    dynamoConfig = dynamoConfig
  )

  val router = new Router(
    progressTracker = progressTracker,
    progressStarter = new ProgressStarter(
      progressTracker = progressTracker,
      snsWriter = snsWriter
    ),
    httpMetrics = httpMetrics,
    httpServerConfig = httpServerConfig,
    contextURL = contextURL
  )

  def run(): Future[Http.HttpTerminated] =
    WellcomeHttpApp.run(
      httpServerConfig = httpServerConfig,
      router = router
    )
}
