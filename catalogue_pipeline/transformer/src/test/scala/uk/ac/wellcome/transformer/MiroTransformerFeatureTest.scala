package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.messaging.test.fixtures.{MessageInfo, SNS, SQS}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.storage.test.fixtures.S3
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class MiroTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with MiroTransformableWrapper
    with TransformableMessageUtils {

  it("transforms miro records and publishes the result to the given topic") {
    val miroID = "M0000001"
    val title = "A guide for a giraffe"

    val secondMiroID = "M0000002"
    val secondTitle = "A song about a snake"

    withLocalSnsTopic { topicArn =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { bucket =>
          sendMiroImageToSQS(
            miroID = miroID,
            data = shouldNotTransformMessage(title),
            bucket = bucket,
            queue = queue
          )

          sendMiroImageToSQS(
            miroID = secondMiroID,
            data = shouldTransformMessage(secondTitle),
            bucket = bucket,
            queue = queue
          )

          val flags: Map[String, String] = Map(
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags(bucket) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topicArn)
              snsMessages.length shouldBe >=(2)

              assertSNSMessageContains(
                snsMessages.head,
                secondMiroID,
                secondTitle)
            }
          }
        }
      }
    }
  }

  private def assertSNSMessageContains(snsMessage: MessageInfo,
                                       miroID: String,
                                       imageTitle: String) = {
    val parsedWork =
      JsonUtil.fromJson[UnidentifiedWork](snsMessage.message).get
    parsedWork.identifiers.head.value shouldBe miroID
    parsedWork.title shouldBe Some(imageTitle)
  }

  def shouldTransformMessage(imageTitle: String) =
    buildJSONForWork(s""""image_title": "$imageTitle"""")

  def shouldNotTransformMessage(imageTitle: String) = s"""{
          "image_title": "$imageTitle",
          "image_cleared": "N",
          "image_copyright_cleared": "N",
          "image_tech_file_size": ["100000"]
        }"""

  private def sendMiroImageToSQS(
    miroID: String,
    data: String,
    bucket: Bucket,
    queue: Queue
  ) = {
    val miroTransformable =
      MiroTransformable(
        sourceId = miroID,
        MiroCollection = "Images-A",
        data = data
      )

    val sqsMessage =
      hybridRecordSqsMessage(
        message = JsonUtil.toJson(miroTransformable).get,
        sourceName = "miro",
        version = 1,
        s3Client = s3Client,
        bucket = bucket
      )

    sqsClient.sendMessage(queue.url, JsonUtil.toJson(sqsMessage).get)
  }

}
