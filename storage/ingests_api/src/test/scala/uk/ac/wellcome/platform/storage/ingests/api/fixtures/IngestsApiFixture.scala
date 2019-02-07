package uk.ac.wellcome.platform.storage.ingests.api.fixtures

import java.net.URL

import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archive.common.fixtures.{
  HttpFixtures,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.http.HttpMetrics
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.platform.storage.ingests.api.IngestsApi
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}

import scala.concurrent.ExecutionContext.Implicits.global

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

  val contextURL = new URL(
    "http://api.wellcomecollection.org/storage/v1/context.json")

  val metricsName = "IngestsApiFixture"

  private def withApp[R](
    table: Table,
    topic: Topic,
    metricsSender: MetricsSender)(testWith: TestWith[IngestsApi, R]): R =
    withSNSWriter(topic) { snsWriter =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val httpMetrics = new HttpMetrics(
            name = metricsName,
            metricsSender = metricsSender
          )

          val ingestsApi = new IngestsApi(
            dynamoClient = dynamoDbClient,
            dynamoConfig = createDynamoConfigWith(table),
            snsWriter = snsWriter,
            httpMetrics = httpMetrics,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )

          ingestsApi.run()

          testWith(ingestsApi)
        }
      }
    }

  def withBrokenApp[R](
    testWith: TestWith[(Table, Topic, MetricsSender, String), R]): R = {
    withLocalSnsTopic { topic =>
      val table = Table("does-not-exist", index = "does-not-exist")
      withMockMetricSender { metricsSender =>
        withApp(table, topic, metricsSender) { _ =>
          testWith(
            (table, topic, metricsSender, httpServerConfig.externalBaseURL))
        }
      }
    }
  }

  def withConfiguredApp[R](
    testWith: TestWith[(Table, Topic, MetricsSender, String), R]): R = {
    withLocalSnsTopic { topic =>
      withProgressTrackerTable { table =>
        withMockMetricSender { metricsSender =>
          withApp(table, topic, metricsSender) { _ =>
            testWith(
              (table, topic, metricsSender, httpServerConfig.externalBaseURL))
          }
        }
      }
    }
  }
}
