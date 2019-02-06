package uk.ac.wellcome.platform.snapshot_generator.services

import java.io.File

import akka.http.scaladsl.model.Uri
import akka.stream.alpakka.s3.S3Exception
import com.amazonaws.services.s3.model.GetObjectRequest
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.{ElasticClient, JavaClientExceptionWrapper}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.json.DisplayJsonUtil
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models.{
  ApiVersions,
  V1WorksIncludes,
  V2WorksIncludes
}
import uk.ac.wellcome.elasticsearch.ElasticClientBuilder
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.snapshot_generator.fixtures.{
  AkkaS3,
  SnapshotServiceFixture
}
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

class SnapshotServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with GzipUtils
    with IntegrationPatience
    with SnapshotServiceFixture
    with WorksGenerators {

  def withFixtures[R](
    testWith: TestWith[(SnapshotService, Index, Index, Bucket), R]): R =
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withS3AkkaClient { s3Client =>
          withLocalWorksIndex { indexV1 =>
            withLocalWorksIndex { indexV2 =>
              withLocalS3Bucket { bucket =>
                withSnapshotService(s3Client, indexV1, indexV2) {
                  snapshotService =>
                    {
                      testWith((snapshotService, indexV1, indexV2, bucket))
                    }
                }
              }
            }
          }
        }
      }
    }

  it("completes a V1 snapshot generation") {
    withFixtures {
      case (snapshotService: SnapshotService, indexV1, _, publicBucket) =>
        val visibleWorks = createIdentifiedWorks(count = 3)
        val notVisibleWorks = createIdentifiedInvisibleWorks(count = 1)

        val works = visibleWorks ++ notVisibleWorks

        insertIntoElasticsearch(indexV1, works: _*)

        val publicObjectKey = "target.txt.gz"

        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v1
        )

        val future = snapshotService.generateSnapshot(snapshotJob)

        whenReady(future) { result =>
          val downloadFile =
            File.createTempFile("snapshotServiceTest", ".txt.gz")
          s3Client.getObject(
            new GetObjectRequest(publicBucket.name, publicObjectKey),
            downloadFile)

          val contents = readGzipFile(downloadFile.getPath)
          val expectedContents = visibleWorks
            .map {
              DisplayWorkV1(_, includes = V1WorksIncludes.includeAll())
            }
            .map {
              DisplayJsonUtil.toJson(_)
            }
            .mkString("\n") + "\n"

          contents shouldBe expectedContents

          result shouldBe CompletedSnapshotJob(
            snapshotJob = snapshotJob,
            targetLocation =
              s"http://localhost:33333/${publicBucket.name}/$publicObjectKey"
          )
        }
    }
  }

  it("completes a V2 snapshot generation") {
    withFixtures {
      case (snapshotService: SnapshotService, _, indexV2, publicBucket) =>
        val visibleWorks = createIdentifiedWorks(count = 4)
        val notVisibleWorks = createIdentifiedInvisibleWorks(count = 2)

        val works = visibleWorks ++ notVisibleWorks

        insertIntoElasticsearch(indexV2, works: _*)

        val publicObjectKey = "target.txt.gz"

        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v2
        )

        val future = snapshotService.generateSnapshot(snapshotJob)

        whenReady(future) { result =>
          val downloadFile =
            File.createTempFile("snapshotServiceTest", ".txt.gz")
          s3Client.getObject(
            new GetObjectRequest(publicBucket.name, publicObjectKey),
            downloadFile)

          val contents = readGzipFile(downloadFile.getPath)
          val expectedContents = visibleWorks
            .map {
              DisplayWorkV2(_, includes = V2WorksIncludes.includeAll())
            }
            .map {
              DisplayJsonUtil.toJson(_)
            }
            .mkString("\n") + "\n"

          contents shouldBe expectedContents

          result shouldBe CompletedSnapshotJob(
            snapshotJob = snapshotJob,
            targetLocation =
              s"http://localhost:33333/${publicBucket.name}/$publicObjectKey"
          )
        }
    }

  }

  it("completes a snapshot generation of an index with more than 10000 items") {
    withFixtures {
      case (snapshotService: SnapshotService, indexV1, _, publicBucket) =>
        val works = (1 to 11000).map { id =>
          createIdentifiedWorkWith(
            title = randomAlphanumeric(length = 1500)
          )
        }

        insertIntoElasticsearch(indexV1, works: _*)

        val publicObjectKey = "target.txt.gz"
        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v1
        )

        val future = snapshotService.generateSnapshot(snapshotJob)

        whenReady(future) { result =>
          val downloadFile =
            File.createTempFile("snapshotServiceTest", ".txt.gz")
          s3Client.getObject(
            new GetObjectRequest(publicBucket.name, publicObjectKey),
            downloadFile)

          val contents = readGzipFile(downloadFile.getPath)
          val expectedContents = works
            .map {
              DisplayWorkV1(_, includes = V1WorksIncludes.includeAll())
            }
            .map {
              DisplayJsonUtil.toJson(_)
            }
            .mkString("\n") + "\n"

          contents shouldBe expectedContents

          result shouldBe CompletedSnapshotJob(
            snapshotJob = snapshotJob,
            targetLocation =
              s"http://localhost:33333/${publicBucket.name}/$publicObjectKey"
          )
        }
    }
  }

  it("returns a failed future if the S3 upload fails") {
    withFixtures {
      case (snapshotService: SnapshotService, indexV1, _, _) =>
        val works = createIdentifiedWorks(count = 3)

        insertIntoElasticsearch(indexV1, works: _*)

        val snapshotJob = SnapshotJob(
          publicBucketName = "wrongBukkit",
          publicObjectKey = "target.json.gz",
          apiVersion = ApiVersions.v1
        )

        val future = snapshotService.generateSnapshot(snapshotJob)

        whenReady(future.failed) { result =>
          result shouldBe a[S3Exception]
        }
    }
  }

  it("returns a failed future if it fails reading from elasticsearch") {
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withS3AkkaClient { s3Client =>
          val brokenElasticClient: ElasticClient = ElasticClientBuilder.create(
            hostname = "localhost",
            port = 8888,
            protocol = "http",
            username = "elastic",
            password = "changeme"
          )

          withSnapshotService(
            s3Client,
            indexV1 = "wrong-index",
            indexV2 = "wrong-index",
            elasticClient = brokenElasticClient) { brokenSnapshotService =>
            val snapshotJob = SnapshotJob(
              publicBucketName = "bukkit",
              publicObjectKey = "target.json.gz",
              apiVersion = ApiVersions.v1
            )

            val future = brokenSnapshotService.generateSnapshot(snapshotJob)

            whenReady(future.failed) { result =>
              result shouldBe a[JavaClientExceptionWrapper]
            }
          }
        }
      }
    }
  }

  describe("buildLocation") {
    it("creates the correct object location in tests") {
      withFixtures {
        case (snapshotService: SnapshotService, _, _, _) =>
          snapshotService.buildLocation(
            bucketName = "bukkit",
            objectKey = "snapshot.json.gz"
          ) shouldBe Uri("http://localhost:33333/bukkit/snapshot.json.gz")
      }
    }

    it("creates the correct object location with the default S3 endpoint") {
      withActorSystem { implicit actorSystem =>
        withMaterializer(actorSystem) { implicit materializer =>
          withS3AkkaClient(endpoint = "") { s3Client =>
            withSnapshotService(
              s3Client,
              indexV1 = "indexv1",
              indexV2 = "indexv2") { snapshotService =>
              snapshotService.buildLocation(
                bucketName = "bukkit",
                objectKey = "snapshot.json.gz"
              ) shouldBe Uri("s3://bukkit/snapshot.json.gz")
            }
          }
        }
      }
    }
  }
}
