package uk.ac.wellcome.platform.snapshot_convertor

import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.platform.snapshot_convertor.models.{
  CompletedConversionJob,
  ConversionJob
}
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, ExtendedPatience}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import io.circe.parser.parse
import uk.ac.wellcome.platform.snapshot_convertor.fixtures.ExampleDump
import scala.util.Try

import scala.io.Source
import scala.util.control.NonFatal
import java.util.zip.GZIPInputStream
import java.io.BufferedInputStream

class SnapshotConvertorFeatureTest
    extends FunSpec
    with AmazonCloudWatchFlag
    with SqsFixtures
    with SnsFixtures
    with S3
    with ExampleDump
    with fixtures.Server
    with ScalaFutures
    with Matchers
    with Eventually
    with ExtendedPatience {

  it(
    "converts a gzipped elasticdump from S3 into the correct format in the target bucket") {
    withLocalS3Bucket { bucketName =>
      withLocalSqsQueue { queueUrl =>
        withLocalSnsTopic { topicArn =>
          withExampleDump(bucketName) { key =>
            val flags = snsLocalFlags(topicArn) ++ sqsLocalFlags(queueUrl) ++ s3LocalFlags(
              bucketName)

            withServer(flags) { _ =>
              val conversionJob = ConversionJob(
                bucketName = bucketName,
                objectKey = key
              )

              val sqsMessage = SQSMessage(
                None,
                toJson(conversionJob).get,
                "topic",
                "message",
                "now"
              )

              sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

              val expectedLocation =
                s"$localS3EndpointUrl/$bucketName/target.txt.gz"

              val expectedCompletedConversionJob = CompletedConversionJob(
                conversionJob = conversionJob,
                targetLocation = expectedLocation
              )

              eventually {
                val messages = listMessagesReceivedFromSNS(topicArn)

                val completedJobs = messages.map { m =>
                  parse(m.message).right.get
                    .as[CompletedConversionJob]
                    .right
                    .get
                }

                completedJobs should contain only (expectedCompletedConversionJob)

                val inputStream = s3Client
                  .getObject(bucketName, "target.txt.gz")
                  .getObjectContent

                val jsons =
                  scala.io.Source
                    .fromInputStream(new GZIPInputStream(
                      new BufferedInputStream(inputStream)))
                    .mkString
                    .split("\n")
                    .map(parse(_))
                    .toSeq

                jsons should contain only parse(expectedDisplayWork)

              }

            }

          }
        }
      }
    }
  }

}
