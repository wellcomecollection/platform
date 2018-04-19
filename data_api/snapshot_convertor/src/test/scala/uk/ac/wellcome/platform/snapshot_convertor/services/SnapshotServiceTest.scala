package uk.ac.wellcome.platform.snapshot_convertor.services

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.S3Exception
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.display.models.{AllWorksIncludes, WorksUtil}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.{IdentifiedWork, IdentifierSchemes, Period, SourceIdentifier}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedSnapshotJob, SnapshotJob}
import uk.ac.wellcome.platform.snapshot_convertor.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, S3, TestWith}
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
    materializer: ActorMaterializer,
    s3AkkaClient: S3Client, indexNameV1: String, indexNameV2: String)(testWith: TestWith[SnapshotService, R]) = {
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
              withSnapshotService(actorSystem, actorMaterialiser, s3Client, indexNameV1, indexNameV2) { snapshotService => {
                testWith((snapshotService, indexNameV1, indexNameV2, bucket))
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
        val notVisibleWorks = createWorks(count = 1,start = 4, visible = false)

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
    withFixtures { case (snapshotService: SnapshotService, _, indexNameV2, publicBucket) =>
        val visibleWorks = createWorks(count = 4)
        val notVisibleWorks = createWorks(count = 2, start = 5, visible = false)

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
              File.createTempFile("convertorServiceTest", ".txt.gz")
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

  // This test is meant to catch an error we saw when we first turned on
  // the snapshot convertor:
  //
  //    akka.http.scaladsl.model.EntityStreamSizeException:
  //    EntityStreamSizeException: actual entity size (Some(19403836)) exceeded
  //    content length limit (8388608 bytes)! You can configure this by setting
  //    `akka.http.[server|client].parsing.max-content-length` or calling
  //    `HttpEntity.withSizeLimit` before materializing the dataBytes stream.
  //
  // With the original code, we were unable to read anything more than
  // an 8MB file from S3.  This test deliberately creates a very large file,
  // and tries to stream it back out.
  //
  it("completes a very large snapshot generation successfully") {
    withFixtures {
      case (snapshotService: SnapshotService, indexNameV1, _, publicBucket) =>
        // Create a collection of works.  The use of Random is meant
        // to increase the entropy of works, and thus the degree to
        // which they can be gzip-compressed -- so we can cross the
        // 8MB boundary with a shorter list!
        val works = (1 to 5000).map { version =>
          IdentifiedWork(
            canonicalId = Random.alphanumeric.take(7).mkString,
            title = Some(Random.alphanumeric.take(1500).mkString),
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.miroImageNumber,
              ontologyType = itemType,
              value = Random.alphanumeric.take(10).mkString
            ),
            description = Some(Random.alphanumeric.take(2500).mkString),
            publicationDate = Some(Period(label = version.toString)),
            version = version
          )
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
              File.createTempFile("convertorServiceTest", ".txt.gz")
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

  it("returns a failed future if the S3 upload fails") {
    withFixtures {
      case (snapshotService: SnapshotService,indexNameV1,_, publicBucket) =>
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

  }
}
