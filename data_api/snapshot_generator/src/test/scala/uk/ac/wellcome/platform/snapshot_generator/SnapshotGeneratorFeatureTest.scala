package uk.ac.wellcome.platform.snapshot_generator

import java.io.File

import com.amazonaws.services.s3.model.GetObjectRequest
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.platform.snapshot_generator.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.SNS.Topic
import uk.ac.wellcome.test.fixtures.SQS.Queue
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.versions.ApiVersions

class SnapshotGeneratorFeatureTest
    extends FunSpec
    with Eventually
    with Matchers
    with Akka
    with AkkaS3
    with S3
    with SNS
    with SQS
    with GzipUtils
    with fixtures.Server
    with JsonTestUtil
    with ExtendedPatience
    with ElasticsearchFixtures {

  val itemType = "work"
  def withFixtures[R](
    testWith: TestWith[(Queue, Topic, String, String, Bucket), R]) =
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalElasticsearchIndex(itemType = itemType) { indexNameV1 =>
          withLocalElasticsearchIndex(itemType = itemType) { indexNameV2 =>
            withLocalS3Bucket { bucket =>
              testWith((queue, topic, indexNameV1, indexNameV2, bucket))
            }
          }
        }
      }
    }

  it("completes a snapshot generation successfully") {
    withFixtures {
      case (queue, topic, indexNameV1, indexNameV2, publicBucket) =>
        val flags = snsLocalFlags(topic) ++ sqsLocalFlags(queue) ++ s3LocalFlags(
          publicBucket) ++ esLocalFlags(indexNameV1, indexNameV2, itemType)

        withServer(flags) { _ =>
          // Create a collection of works.  These three differ by version,
          // if not anything more interesting!
          val works = (1 to 3).map { version =>
            IdentifiedWork(
              canonicalId = s"rbfhv6b4$version",
              title = Some("Rumblings from a rambunctious rodent"),
              sourceIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.miroImageNumber,
                ontologyType = "work",
                value = "R0060400"
              ),
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

          val message = SQSMessage(
            subject = Some("Sent from SnapshotGeneratorFeatureTest"),
            body = toJson(snapshotJob).get,
            messageType = "json",
            topic = topic.arn,
            timestamp = "now"
          )

          sqsClient.sendMessage(queue.url, toJson(message).get)

          eventually {

            val downloadFile =
              File.createTempFile("snapshotGeneratorFeatureTest", ".txt.gz")

            s3Client.getObject(
              new GetObjectRequest(publicBucket.name, publicObjectKey),
              downloadFile)

            val actualJsonLines: List[String] =
              readGzipFile(downloadFile.getPath).split("\n").toList

            val expectedJsonLines = works.map { work =>
              s"""{
                 |  "id": "${work.canonicalId}",
                 |  "title": "${work.title.get}",
                 |  "identifiers": [ ],
                 |  "creators": [ ],
                 |  "genres": [ ],
                 |  "subjects": [ ],
                 |  "items": [ ],
                 |  "publishers": [ ],
                 |  "placesOfPublication": [ ],
                 |  "type": "Work"
                   }""".stripMargin
            }

            actualJsonLines.zip(expectedJsonLines).foreach {
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

  }
}
