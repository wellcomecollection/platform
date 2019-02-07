package uk.ac.wellcome.platform.storage.ingests.api.fixtures

import java.net.URL

import akka.stream.QueueOfferResult
import org.mockito.Mockito.{atLeastOnce, verify}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.{
  HttpFixtures,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.platform.storage.ingests.api.IngestsApi
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IngestsApiFixture
    extends S3
    with RandomThings
    with LocalDynamoDb
    with ScalaFutures
    with ProgressTrackerFixture
    with ProgressGenerators
    with SNS
    with HttpFixtures
    with Messaging {

  private def withApp[R](
    table: Table,
    topic: Topic,
    metricsSender: MetricsSender,
    httpServerConfig: HTTPServerConfig,
    contextURL: URL)(testWith: TestWith[IngestsApi, R]): R =
    withSNSWriter(topic) { snsWriter =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val ingestsApi = new IngestsApi(
            dynamoClient = dynamoDbClient,
            dynamoConfig = createDynamoConfigWith(table),
            snsWriter = snsWriter,
            metricsSender = metricsSender,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )

          ingestsApi.run()

          testWith(ingestsApi)
        }
      }
    }

  def withConfiguredApp[R](testWith: TestWith[(Table, Topic, MetricsSender, String), R]): R = {
    val contextURL = new URL(
      "http://api.wellcomecollection.org/storage/v1/context.json")

    val httpServerConfig = createHTTPServerConfig

    withLocalSnsTopic { topic =>
      withProgressTrackerTable { table =>
        withMockMetricSender { metricsSender =>
          withApp(table, topic, metricsSender, httpServerConfig, contextURL) { _ =>
            testWith((table, topic, metricsSender, httpServerConfig.externalBaseURL))
          }
        }
      }
    }
  }

  def assertMetricSent(metricsSender: MetricsSender, result: String): Future[QueueOfferResult] =
    verify(metricsSender, atLeastOnce())
      .incrementCount(metricName = s"IngestsApi_HttpResponse_result")
}
