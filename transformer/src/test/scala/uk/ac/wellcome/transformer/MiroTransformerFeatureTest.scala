package uk.ac.wellcome.transformer

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{MiroTransformable, Work}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest
    extends FunSpec
    with TransformerFeatureTest
    with Matchers {

  val queueUrl: String = createQueueAndReturnUrl("test_miro_transformer")
  override val flags: Map[String, String] = Map(
    "transformer.source" -> "MiroData",
    "aws.region" -> "eu-west-1",
    "aws.sqs.queue.url" -> queueUrl,
    "aws.sqs.waitTime" -> "1",
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "miro-transformer"
  )

  it(
    "should poll the Dynamo stream for Miro records, transform into Work instances, and push them into the id_minter SNS topic") {
    val miroID = "M0000001"
    val label = "A guide for a giraffe"
    sendMiroImageToSQS(miroID, label)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)
      assertSNSMessageContains(snsMessages.head, miroID, label)
    }

    val secondMiroID = "M0000002"
    val secondLabel = "A song about a snake"
    sendMiroImageToSQS(secondMiroID, secondLabel)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (2)

      assertSNSMessageContains(snsMessages.head, miroID, label)
      assertSNSMessageContains(snsMessages.tail.head,
                               secondMiroID,
                               secondLabel)
    }
  }

  private def assertSNSMessageContains(snsMessage: MessageInfo,
                                       miroID: String,
                                       imageTitle: String) = {
    val parsedWork = JsonUtil.fromJson[Work](snsMessage.message).get
    parsedWork.identifiers.head.value shouldBe miroID
    parsedWork.label shouldBe imageTitle
  }

  private def sendMiroImageToSQS(miroID: String, imageTitle: String) = {
    val miroTransformable = MiroTransformable(miroID,
      "Images-A",
      s"""{
          "image_title": "$imageTitle",
          "image_cleared": "Y",
          "image_copyright_cleared": "Y"
        }""")

    val sqsMessage = SQSMessage(Some("subject"),
      JsonUtil.toJson(miroTransformable).get,
      "topic",
      "messageType",
      "timestamp")

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)
  }

}
