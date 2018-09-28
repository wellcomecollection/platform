package uk.ac.wellcome.platform.snapshot_generator

import java.io.File

import com.amazonaws.services.s3.model.GetObjectRequest
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.models.v1.DisplayV1SerialisationTestBase
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.monitoring.fixtures.CloudWatch
import uk.ac.wellcome.platform.snapshot_generator.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

class SnapshotGeneratorFeatureTest
    extends FunSpec
    with Eventually
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with SNS
    with SQS
    with fixtures.Server
    with CloudWatch
    with GzipUtils
    with JsonAssertions
    with IntegrationPatience
    with ElasticsearchFixtures
    with DisplayV1SerialisationTestBase
    with WorksGenerators {

  val itemType = "work"

  it("completes a snapshot generation") {
    withFixtures {
      case (queue, topic, indexNameV1, _, publicBucket: Bucket) =>
        val works = createIdentifiedWorks(count = 3)

        insertIntoElasticsearch(indexNameV1, itemType, works: _*)

        val publicObjectKey = "target.txt.gz"

        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v1
        )

        sendNotificationToSQS(queue = queue, message = snapshotJob)

        eventually {
          val downloadFile =
            File.createTempFile("snapshotGeneratorFeatureTest", ".txt.gz")

          s3Client.getObject(
            new GetObjectRequest(publicBucket.name, publicObjectKey),
            downloadFile)

          val actualJsonLines: List[String] =
            readGzipFile(downloadFile.getPath).split("\n").toList

          val expectedJsonLines = works.sortBy { _.canonicalId }.map { work =>
            s"""{
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title}",
                 |  "identifiers": [ ${identifier(work.sourceIdentifier)} ],
                 |  "creators": [ ],
                 |  "genres": [ ],
                 |  "subjects": [ ],
                 |  "items": [ ],
                 |  "publishers": [ ],
                 |  "placesOfPublication": [ ],
                 |  "type": "Work"
                   }""".stripMargin
          }

          actualJsonLines.sorted.zip(expectedJsonLines).foreach {
            case (actualLine, expectedLine) =>
              assertJsonStringsAreEqual(actualLine, expectedLine)
          }

          val receivedMessages = listMessagesReceivedFromSNS(topic)
          receivedMessages.size should be >= 1

          val expectedJob = CompletedSnapshotJob(
            snapshotJob = snapshotJob,
            targetLocation =
              s"http://localhost:33333/${publicBucket.name}/$publicObjectKey"
          )
          val actualJob =
            fromJson[CompletedSnapshotJob](receivedMessages.head.message).get
          actualJob shouldBe expectedJob
        }
    }

  }

  def withFixtures[R](
    testWith: TestWith[(Queue, Topic, String, String, Bucket), R]) =
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            withLocalS3Bucket { bucket =>
              val flags = snsLocalFlags(topic) ++ sqsLocalFlags(queue) ++ displayEsLocalFlags(
                indexNameV1,
                indexNameV2,
                itemType) ++ s3ClientLocalFlags
              withServer(flags) { _ =>
                testWith((queue, topic, indexNameV1, indexNameV2, bucket))
              }
            }
          }
        }
      }
    }

}
