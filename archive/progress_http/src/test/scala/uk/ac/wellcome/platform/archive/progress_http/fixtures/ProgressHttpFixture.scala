package uk.ac.wellcome.platform.archive.progress_http.fixtures

import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.stream.Materializer
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS}
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.progress.fixtures.{
  ProgressGenerators,
  ProgressTrackerFixture
}
import uk.ac.wellcome.platform.archive.progress_http.ProgressHTTP
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.{LocalDynamoDb, S3}
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait ProgressHttpFixture
    extends S3
    with RandomThings
    with LocalDynamoDb
    with ScalaFutures
    with ProgressTrackerFixture
    with ProgressGenerators
    with SNS
    with Messaging {

  def withApp[R](table: Table,
                 topic: Topic,
                 httpServerConfig: HTTPServerConfig,
                 contextURL: URL)(testWith: TestWith[ProgressHTTP, R]): R =
    withSNSWriter(topic) { snsWriter =>
      withActorSystem { actorSystem =>
        withMaterializer(actorSystem) { materializer =>
          val progressHTTP = new ProgressHTTP(
            dynamoClient = dynamoDbClient,
            dynamoConfig = createDynamoConfigWith(table),
            snsWriter = snsWriter,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )(
            actorSystem = actorSystem,
            materializer = materializer,
            executionContext = actorSystem.dispatcher
          )

          progressHTTP.run()

          testWith(progressHTTP)
        }
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(Table, Topic, String), R]): R = {
    val host = "localhost"
    val port = randomPort
    val externalBaseURL = s"http://$host:$port"
    val contextURL = new URL(
      "http://api.wellcomecollection.org/storage/v1/context.json")

    val httpServerConfig = HTTPServerConfig(
      host = host,
      port = port,
      externalBaseURL = externalBaseURL
    )

    withLocalSnsTopic { topic =>
      withProgressTrackerTable { table =>
        withApp(table, topic, httpServerConfig, contextURL) { _ =>
          testWith((table, topic, externalBaseURL))
        }
      }
    }
  }

  def getT[T](entity: HttpEntity)(implicit decoder: Decoder[T],
                                  materializer: Materializer): T = {
    val timeout = 300.millis

    val stringBody = entity
      .toStrict(timeout)
      .map(_.data)
      .map(_.utf8String)
      .value
      .get
      .get
    fromJson[T](stringBody).get
  }

  def whenRequestReady[R](r: HttpRequest)(testWith: TestWith[HttpResponse, R]): R =
    withActorSystem { implicit actorSystem =>
      val request = Http().singleRequest(r)
      whenReady(request) { (response: HttpResponse) =>
        testWith(response)
      }
    }
}
