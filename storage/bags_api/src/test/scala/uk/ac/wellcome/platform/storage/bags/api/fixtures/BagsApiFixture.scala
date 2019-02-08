package uk.ac.wellcome.platform.storage.bags.api.fixtures

import java.net.URL

import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.common.fixtures.{
  HttpFixtures,
  RandomThings
}
import uk.ac.wellcome.platform.archive.common.http.HttpMetrics
import uk.ac.wellcome.platform.archive.registrar.fixtures.StorageManifestVHSFixture
import uk.ac.wellcome.platform.storage.bags.api.BagsApi
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.Akka

import scala.concurrent.ExecutionContext.Implicits.global

trait BagsApiFixture
    extends RandomThings
    with ScalaFutures
    with StorageManifestVHSFixture
    with HttpFixtures
    with MetricsSenderFixture
    with Akka {

  val metricsName = "BagsApiFixture"

  val contextURL = new URL(
    "http://api.wellcomecollection.org/storage/v1/context.json")

  private def withApp[R](metricsSender: MetricsSender, vhs: StorageManifestVHS)(
    testWith: TestWith[BagsApi, R]): R =
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        val httpMetrics = new HttpMetrics(
          name = metricsName,
          metricsSender = metricsSender
        )

        val bagsApi = new BagsApi(
          vhs = vhs,
          httpMetrics = httpMetrics,
          httpServerConfig = httpServerConfig,
          contextURL = contextURL
        )

        bagsApi.run()

        testWith(bagsApi)
      }
    }

  def withConfiguredApp[R](
    testWith: TestWith[(StorageManifestVHS, MetricsSender, String), R]): R =
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withStorageManifestVHS(table, bucket) { vhs =>
          withMockMetricSender { metricsSender =>
            withApp(metricsSender, vhs) { _ =>
              testWith((vhs, metricsSender, httpServerConfig.externalBaseURL))
            }
          }
        }
      }
    }

  def withBrokenApp[R](
    testWith: TestWith[(StorageManifestVHS, MetricsSender, String), R]): R = {
    val bucket = Bucket("does-not-exist")
    val table = Table("does-not-exist", index = "does-not-exist")
    withStorageManifestVHS(table, bucket) { vhs =>
      withMockMetricSender { metricsSender =>
        withApp(metricsSender, vhs) { _ =>
          testWith((vhs, metricsSender, httpServerConfig.externalBaseURL))
        }
      }
    }
  }
}
