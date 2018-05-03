package uk.ac.wellcome.platform.snapshot_generator.services

import java.io.File

import akka.actor.ActorSystem
import akka.stream.alpakka.s3.S3Exception
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.elasticsearch.client.ResponseException
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models.AllWorksIncludes
import uk.ac.wellcome.display.test.util.WorksUtil
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.internal.IdentifierSchemes.sierraSystemNumber
import uk.ac.wellcome.models.work.internal.{IdentifiedWork, SourceIdentifier}
import uk.ac.wellcome.platform.snapshot_generator.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.versions.ApiVersions

import scala.util.Random

class SnapshotServiceTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with GzipUtils
    with ExtendedPatience
    with ElasticsearchFixtures
    with WorksUtil {

  val mapper = new ObjectMapper with ScalaObjectMapper

  val itemType = "work"

  private def withSnapshotService[R](
    actorSystem: ActorSystem,
    s3AkkaClient: S3Client,
    indexNameV1: String,
    indexNameV2: String)(testWith: TestWith[SnapshotService, R]) = {
    val snapshotService = new SnapshotService(
      actorSystem = actorSystem,
      elasticClient = elasticClient,
      akkaS3Client = s3AkkaClient,
      s3Endpoint = localS3EndpointUrl,
      esIndexV1 = indexNameV1,
      esIndexV2 = indexNameV2,
      esType = itemType,
      objectMapper = mapper
    )

    testWith(snapshotService)
  }

  def withFixtures[R](
    testWith: TestWith[(SnapshotService, String, String, Bucket), R]) =
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { actorMaterialiser =>
        withS3AkkaClient(actorSystem, actorMaterialiser) { s3Client =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
            withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
              withLocalS3Bucket { bucket =>
                withSnapshotService(
                  actorSystem,
                  s3Client,
                  indexNameV1,
                  indexNameV2) { snapshotService =>
                  {
                    testWith(
                      (snapshotService, indexNameV1, indexNameV2, bucket))
                  }
                }
              }
            }
          }
        }
      }
    }

  it("completes a V1 snapshot generation successfully") {
    withFixtures {
      case (snapshotService: SnapshotService, indexNameV1, _, publicBucket) =>
        val visibleWorks = createWorks(count = 3)
        val notVisibleWorks =
          createWorks(count = 1, start = 4, visible = false)

        val works = visibleWorks ++ notVisibleWorks

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

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
              DisplayWorkV1(_, includes = AllWorksIncludes())
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

  it("completes a V2 snapshot generation successfully") {
    withFixtures {
      case (snapshotService: SnapshotService, _, indexNameV2, publicBucket) =>
        val visibleWorks = createWorks(count = 4)
        val notVisibleWorks =
          createWorks(count = 2, start = 5, visible = false)

        val works = visibleWorks ++ notVisibleWorks

        insertIntoElasticsearch(indexNameV2, itemType, works: _*)

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
              DisplayWorkV2(_, includes = AllWorksIncludes())
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
          workWith(
            canonicalId = id.toString,
            title = Random.alphanumeric.take(1500).mkString)
        }

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

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
              DisplayWorkV1(_, includes = AllWorksIncludes())
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

  it("fails a snapshot generation if one of the items in the index is invalid") {
    withFixtures {
      case (snapshotService: SnapshotService, indexNameV1, _, publicBucket) =>
        val validWorks = createWorks(count = 3)

        val invalidWork = IdentifiedWork(
          canonicalId = "invalidwork",
          sourceIdentifier = SourceIdentifier(
            identifierScheme = sierraSystemNumber,
            ontologyType = "Work",
            value = "123"),
          version = 1,
          title = None,
          visible = true
        )

        val works = validWorks :+ invalidWork
        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

        val publicObjectKey = "target.txt.gz"
        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v1
        )

        val future = snapshotService.generateSnapshot(snapshotJob)

        whenReady(future.failed) { ex =>
          ex shouldBe a[RuntimeException]
        }
    }
  }

  it("returns a failed future if the S3 upload fails") {
    withFixtures {
      case (snapshotService: SnapshotService, indexNameV1, _, _) =>
        val works = createWorks(count = 3)

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

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
    withActorSystem { actorSystem =>
      withMaterializer(actorSystem) { actorMaterialiser =>
        withS3AkkaClient(actorSystem, actorMaterialiser) { s3Client =>
          withLocalS3Bucket { bucket =>
            val brokenSnapshotService = new SnapshotService(
              actorSystem = actorSystem,
              elasticClient = elasticClient,
              akkaS3Client = s3Client,
              s3Endpoint = localS3EndpointUrl,
              esIndexV1 = "wrong-index",
              esIndexV2 = "wrong-index",
              esType = itemType,
              objectMapper = mapper
            )
            val snapshotJob = SnapshotJob(
              publicBucketName = bucket.name,
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
  }

}
