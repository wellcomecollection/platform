package uk.ac.wellcome.platform.storage.bags.api.fixtures

import java.net.URL

import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.fixtures.{HttpFixtures, RandomThings}
import uk.ac.wellcome.platform.archive.registrar.fixtures.StorageManifestVHSFixture
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.platform.storage.bags.api.BagsApi
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait BagsApiFixture
    extends RandomThings
    with ScalaFutures
    with StorageManifestVHSFixture
    with HttpFixtures
    with Akka {

  def withApp[R](table: Table,
                 bucket: Bucket,
                 s3Prefix: String,
                 httpServerConfig: HTTPServerConfig,
                 contextURL: URL)(testWith: TestWith[BagsApi, R]): R =
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withStorageManifestVHS(table, bucket) { vhs =>
          val bagsApi = new BagsApi(
            vhs = vhs,
            httpServerConfig = httpServerConfig,
            contextURL = contextURL
          )

          bagsApi.run()

          testWith(bagsApi)
        }
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(StorageManifestVHS, String), R]): R = {
    val contextURL = new URL(
      "http://api.wellcomecollection.org/storage/v1/context.json")

    val httpServerConfig = createHTTPServerConfig

    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        val s3Prefix = "archive"
        withStorageManifestVHS(table, bucket, s3Prefix) { vhs =>
          withApp(table, bucket, s3Prefix, httpServerConfig, contextURL) { _ =>
            testWith((vhs, httpServerConfig.externalBaseURL))
          }
        }
      }
    }
  }
}
