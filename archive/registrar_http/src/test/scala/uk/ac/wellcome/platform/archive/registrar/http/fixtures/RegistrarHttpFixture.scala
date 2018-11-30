package uk.ac.wellcome.platform.archive.registrar.http.fixtures

import java.net.URL

import akka.http.scaladsl.model.HttpEntity
import akka.stream.Materializer
import io.circe.Decoder
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.{HttpFixtures, RandomThings}
import uk.ac.wellcome.platform.archive.registrar.fixtures.StorageManifestVHSFixture
import uk.ac.wellcome.platform.archive.registrar.http.RegistrarHTTP
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait RegistrarHttpFixture
    extends RandomThings
    with ScalaFutures
    with StorageManifestVHSFixture
    with HttpFixtures
    with Akka {

  def withApp[R](table: Table,
                 bucket: Bucket,
                 s3Prefix: String,
                 httpServerConfig: HTTPServerConfig,
                 contextURL: URL)(testWith: TestWith[RegistrarHTTP, R]): R =
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { materializer =>
        withStorageManifestVHS(table, bucket) { vhs =>
          val registrarHTTP = new RegistrarHTTP(
            vhs = vhs,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )(
            actorSystem = actorSystem,
            materializer = materializer,
            executionContext = actorSystem.dispatcher
          )

          registrarHTTP.run()

          testWith(registrarHTTP)
        }
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(StorageManifestVHS, String), R]): R = {
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

    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        val s3Prefix = "archive"
        withStorageManifestVHS(table, bucket, s3Prefix) { vhs =>
          withApp(table, bucket, s3Prefix, httpServerConfig, contextURL) { _ =>
            testWith((vhs, externalBaseURL))
          }
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
}
