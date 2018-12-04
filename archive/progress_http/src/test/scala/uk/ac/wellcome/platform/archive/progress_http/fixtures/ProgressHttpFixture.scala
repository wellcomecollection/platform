package uk.ac.wellcome.platform.archive.progress_http.fixtures

import java.net.URL

import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS}
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.{
  HttpFixtures,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.platform.archive.progress_http.ProgressHTTP
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait ProgressHttpFixture
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
    httpServerConfig: HTTPServerConfig,
    contextURL: URL)(testWith: TestWith[ProgressHTTP, R]): R =
    withSNSWriter(topic) { snsWriter =>
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          val progressHTTP = new ProgressHTTP(
            dynamoClient = dynamoDbClient,
            dynamoConfig = createDynamoConfigWith(table),
            snsWriter = snsWriter,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )

          progressHTTP.run()

          testWith(progressHTTP)
        }
      }
    }

  def withConfiguredApp[R](testWith: TestWith[(Table, Topic, String), R]): R = {
    val contextURL = new URL(
      "http://api.wellcomecollection.org/storage/v1/context.json")

    val httpServerConfig = createHTTPServerConfig

    withLocalSnsTopic { topic =>
      withProgressTrackerTable { table =>
        withApp(table, topic, httpServerConfig, contextURL) { _ =>
          testWith((table, topic, httpServerConfig.externalBaseURL))
        }
      }
    }
  }
}
