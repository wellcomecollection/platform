package uk.ac.wellcome.transformer

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{UnidentifiedWork, Work}
import uk.ac.wellcome.models.transformable.{MiroTransformable, Transformable}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.transformer.utils.{
  TransformableSQSMessageUtils,
  TransformerFeatureTest
}
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest
    extends FunSpec
    with TransformerFeatureTest
    with Matchers
    with MiroTransformableWrapper
    with TransformableSQSMessageUtils {
  override lazy val bucketName: String =
    "test-miro-transformer-feature-test-bucket"
  val queueUrl: String = createQueueAndReturnUrl("test_miro_transformer")
  override val flags: Map[String, String] = Map(
    "aws.region" -> "eu-west-1",
    "aws.sqs.queue.url" -> queueUrl,
    "aws.sqs.waitTime" -> "1",
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "miro-transformer",
    "aws.s3.bucketName" -> bucketName
  )

  it("""
      should poll the Dynamo stream for Miro records, transform into Work
      instances, and push them into the id_minter SNS topic where those
      messages are transformable
      """) {

    val miroID = "M0000001"
    val title = "A guide for a giraffe"

    val secondMiroID = "M0000002"
    val secondTitle = "A song about a snake"

    sendMiroImageToSQS(miroID, shouldNotTransformMessage(title))
    sendMiroImageToSQS(secondMiroID, shouldTransformMessage(secondTitle))

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)

      assertSNSMessageContains(snsMessages.head, secondMiroID, secondTitle)
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

  private def sendMiroImageToSQS(miroID: String, data: String) = {
    val miroTransformable =
      MiroTransformable(
        sourceId = miroID,
        MiroCollection = "Images-A",
        data = data
      )

    val sqsMessage =
      hybridRecordSqsMessage(JsonUtil.toJson(miroTransformable).get, "miro")

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)
  }

}
