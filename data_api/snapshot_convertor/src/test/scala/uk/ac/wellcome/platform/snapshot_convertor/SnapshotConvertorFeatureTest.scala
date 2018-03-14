package uk.ac.wellcome.platform.snapshot_convertor

import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.platform.snapshot_convertor.models.{CompletedConversionJob, ConversionJob}
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, ExtendedPatience}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

import scala.io.Source


class SnapshotConvertorFeatureTest
  extends FunSpec
    with AmazonCloudWatchFlag
    with SqsFixtures
    with SnsFixtures
    with S3
    with fixtures.Server
    with ScalaFutures
    with Matchers
    with Eventually
    with ExtendedPatience {

  it("converts a gzipped elasticdump from S3 into the correct format in the target bucket") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalS3Bucket { bucketName =>

          val flags = Map(
            "aws.region" -> "eu-west-1"
          ) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(queueUrl) ++ s3LocalFlags(bucketName) ++ cloudWatchLocalEndpointFlag

          val key = "elastic_dump_example.txt.gz"
          val input = getClass.getResourceAsStream("/elastic_dump_example.txt.gz")
          val metadata = new ObjectMetadata()

          s3Client.putObject(bucketName, key, input, metadata)

          withServer(flags) { _ =>

            val objectKey = "location/of/resource"
            val expectedLocation = "location/of/target"

            val conversionJob = ConversionJob(
              bucketName = bucketName,
              objectKey = key
            )

            val completedConversionJob = CompletedConversionJob(
              conversionJob = conversionJob,
              targetLocation = "location/of/target"
            )


            val sqsMessage = SQSMessage(
              None,
              toJson(conversionJob).get,
              "topic",
              "message",
              "now"
            )

            sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

            eventually {

              val messages = listMessagesReceivedFromSNS(topicArn)

              messages should have size 1

              JsonUtil
                .fromJson[CompletedConversionJob](
                messages.head.message
              ).get shouldBe completedConversionJob

              val s3Object = s3Client.getObject(bucketName, expectedLocation)
              val stream: S3ObjectInputStream = s3Object.getObjectContent

              val outputLines = Source.fromInputStream(stream).getLines.mkString

              true shouldBe false
            }
          }
        }
      }
    }
  }
}
