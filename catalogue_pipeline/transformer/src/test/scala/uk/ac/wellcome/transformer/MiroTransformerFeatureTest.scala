package uk.ac.wellcome.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.{UnidentifiedWork}
import uk.ac.wellcome.models.transformable.{MiroTransformable}
import uk.ac.wellcome.test.fixtures.{S3, SnsFixtures, SqsFixtures}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.test.fixtures.MessageInfo
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SqsFixtures
    with SnsFixtures
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
      withLocalSqsQueue { queueUrl =>
        withLocalS3Bucket { bucketName =>
          sendMiroImageToSQS(
            miroID = miroID,
            data = shouldNotTransformMessage(title),
            bucketName = bucketName,
            queueUrl = queueUrl
          )

          sendMiroImageToSQS(
            miroID = secondMiroID,
            data = shouldTransformMessage(secondTitle),
            bucketName = bucketName,
            queueUrl = queueUrl
          )

          val flags: Map[String, String] = Map(
            "aws.metrics.namespace" -> "sierra-transformer"
          ) ++ s3LocalFlags(bucketName) ++ snsLocalFlags(topicArn) ++ sqsLocalFlags(queueUrl)

          withServer(flags) { _ =>
            eventually {
              val snsMessages = listMessagesReceivedFromSNS(topicArn)
              snsMessages should have size (2)

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
    bucketName: String,
    queueUrl: String
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
        bucketName = bucketName
      )

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)
  }

}
