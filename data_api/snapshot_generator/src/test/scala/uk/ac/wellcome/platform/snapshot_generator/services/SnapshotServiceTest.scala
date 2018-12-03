package uk.ac.wellcome.platform.snapshot_generator.services

import java.io.File

import akka.http.scaladsl.model.Uri
import akka.stream.alpakka.s3.S3Exception
import com.amazonaws.services.s3.model.GetObjectRequest
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models.{ApiVersions, V1WorksIncludes, V2WorksIncludes}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.snapshot_generator.fixtures.SnapshotServiceFixture
import uk.ac.wellcome.platform.snapshot_generator.models.{CompletedSnapshotJob, SnapshotJob}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}

class SnapshotServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Akka
    with GzipUtils
    with IntegrationPatience
    with WorksGenerators
    with SnapshotServiceFixture {

  it("completes a V1 snapshot generation") {
    withFixtures {
      case (snapshotService: SnapshotService, indexNameV1, _, publicBucket) =>
        val visibleWorks = createIdentifiedWorks(count = 3)
        val notVisibleWorks = createIdentifiedInvisibleWorks(count = 1)

        val works = visibleWorks ++ notVisibleWorks

        insertIntoElasticsearch(indexNameV1, works: _*)

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
              mapper.writeValueAsString(_)
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
      case (snapshotService: SnapshotService, _, indexNameV2, publicBucket) =>
        val visibleWorks = createIdentifiedWorks(count = 4)
        val notVisibleWorks = createIdentifiedInvisibleWorks(count = 2)

        val works = visibleWorks ++ notVisibleWorks

        insertIntoElasticsearch(indexNameV2, works: _*)

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
              mapper.writeValueAsString(_)
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
      case (snapshotService: SnapshotService, indexNameV1, _, publicBucket) =>
        val works = (1 to 11000).map { id =>
          createIdentifiedWorkWith(
            title = randomAlphanumeric(length = 1500)
          )
        }

        insertIntoElasticsearch(indexNameV1, works: _*)

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
              mapper.writeValueAsString(_)
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
      case (snapshotService: SnapshotService, indexNameV1, _, _) =>
        val works = createIdentifiedWorks(count = 3)

        insertIntoElasticsearch(indexNameV1, works: _*)

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
        withSnapshotService(
          indexV1name = "wrong-index",
          indexV2name = "wrong-index") { brokenSnapshotService =>
          val snapshotJob = SnapshotJob(
            publicBucketName = "bukkit",
            publicObjectKey = "target.json.gz",
            apiVersion = ApiVersions.v1
          )

          val future = brokenSnapshotService.generateSnapshot(snapshotJob)

          whenReady(future.failed) { result =>
            result shouldBe a[ResponseException]
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
              indexV1name = "indexv1",
              indexV2name = "indexv2") { snapshotService =>
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

  private def withFixtures[R](
    testWith: TestWith[(SnapshotService, String, String, Bucket), R]): R =
    withActorSystem { implicit actorSystem =>
      withMaterializer(actorSystem) { implicit materializer =>
        withLocalElasticsearchIndex { indexV1name =>
          withLocalElasticsearchIndex { indexV2name =>
            withLocalS3Bucket { bucket =>
              withSnapshotService(indexV1name, indexV2name) {
                snapshotService => {
                  testWith(
                    (snapshotService, indexV1name, indexV2name, bucket))
                }
              }
            }
          }
        }
      }
    }
}
