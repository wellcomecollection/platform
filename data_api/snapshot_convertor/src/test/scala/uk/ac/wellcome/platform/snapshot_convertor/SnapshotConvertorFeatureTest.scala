package uk.ac.wellcome.platform.snapshot_convertor

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.display.models.{AllWorksIncludes, DisplayWork}
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
import uk.ac.wellcome.platform.snapshot_convertor.services.ConvertorService
import uk.ac.wellcome.platform.snapshot_convertor.test.utils.GzipUtils
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
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
    with ExtendedPatience {

  it("completes a conversion successfully") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalS3Bucket { bucketName =>
          val flags = snsLocalFlags(topicArn) ++ sqsLocalFlags(queueUrl) ++ s3LocalFlags(
            bucketName)

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

            withGzipCompressedS3Key(bucketName, content) { objectKey =>
              val conversionJob = ConversionJob(
                bucketName = bucketName,
                objectKey = objectKey
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
                  new GetObjectRequest(bucketName, "target.txt.gz"),
                  downloadFile)

                val contents = readGzipFile(downloadFile.getPath)
                val expectedContents = works
                  .map { DisplayWork(_, includes = AllWorksIncludes()) }
                  .map { toJson(_).get }
                  .mkString("\n") + "\n"

                contents shouldBe expectedContents

                val receivedMessages = listMessagesReceivedFromSNS(topicArn)
                receivedMessages should have size 1

                val expectedJob = CompletedConversionJob(
                  conversionJob = conversionJob,
                  targetLocation =
                    s"http://localhost:33333/$bucketName/target.txt.gz"
                )
                val actualJob = fromJson[CompletedConversionJob](receivedMessages.head.message).get
                actualJob shouldBe expectedJob
              }
            }
          }
        }
      }
    }
  }
}
