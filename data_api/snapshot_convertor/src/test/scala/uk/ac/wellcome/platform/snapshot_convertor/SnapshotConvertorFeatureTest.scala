package uk.ac.wellcome.platform.snapshot_convertor

import java.io.File

import com.amazonaws.services.s3.model.GetObjectRequest
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{
  IdentifiedWork,
  IdentifierSchemes,
  SourceIdentifier
}
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.AkkaS3
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.platform.snapshot_convertor.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil._

class SnapshotConvertorFeatureTest
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
    with ExtendedPatience {

  def withFixtures[R] =
    withLocalSqsQueue[R] and
      withLocalSnsTopic[R] and
      withLocalS3Bucket[R] and
      withLocalS3Bucket[R]

  it("completes a conversion successfully") {
    withFixtures {
      case (((queueUrl, topicArn), sourceBucketName), targetBucketName) =>
        val flags = snsLocalFlags(topicArn) ++ sqsLocalFlags(queueUrl) ++ s3LocalFlags(
          sourceBucketName)

        withServer(flags) { _ =>
          // Create a collection of works.  These three differ by version,
          // if not anything more interesting!
          val works = (1 to 3).map { version =>
            IdentifiedWork(
              canonicalId = "rbfhv6b4",
              title = Some("Rumblings from a rambunctious rodent"),
              sourceIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.miroImageNumber,
                ontologyType = "work",
                value = "R0060400"
              ),
              version = version
            )
          }

          val elasticsearchJsons = works.map { work =>
            s"""{"_index": "jett4fvw", "_type": "work", "_id": "${work.canonicalId}", "_score": 1, "_source": ${toJson(
              work).get}}"""
          }
          val content = elasticsearchJsons.mkString("\n")

          val targetObjectKey = "target.txt.gz"

          withGzipCompressedS3Key(sourceBucketName, content) { objectKey =>
            val conversionJob = ConversionJob(
              sourceBucketName = sourceBucketName,
              sourceObjectKey = objectKey,
              targetBucketName = targetBucketName,
              targetObjectKey = targetObjectKey
            )

            val message = SQSMessage(
              subject = Some("Sent from SnapshotConvertorFeatureTest"),
              body = toJson(conversionJob).get,
              messageType = "json",
              topic = topicArn,
              timestamp = "now"
            )

            sqsClient.sendMessage(queueUrl, toJson(message).get)

            eventually {

              val downloadFile =
                File.createTempFile("convertorServiceTest", ".txt.gz")

              s3Client.getObject(
                new GetObjectRequest(targetBucketName, targetObjectKey),
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

              val receivedMessages = listMessagesReceivedFromSNS(topicArn)
              receivedMessages.size should be >= 1

              val expectedJob = CompletedConversionJob(
                conversionJob = conversionJob,
                targetLocation =
                  s"http://localhost:33333/$targetBucketName/$targetObjectKey"
              )
              val actualJob = fromJson[CompletedConversionJob](
                receivedMessages.head.message).get
              actualJob shouldBe expectedJob
            }
          }
        }
    }
  }
}
