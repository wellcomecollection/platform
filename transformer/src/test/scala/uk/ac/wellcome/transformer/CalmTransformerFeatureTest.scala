package uk.ac.wellcome.transformer

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{CalmTransformable, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class  CalmTransformerFeatureTest
    extends FunSpec
    with TransformerFeatureTest
    with Matchers {

  val queueUrl: String = createQueueAndReturnUrl("test_calm_transformer")
  override val flags: Map[String, String] = Map(
    "transformer.source" -> "CalmData",
    "aws.region" -> "eu-west-1",
    "aws.sqs.queue.url" -> queueUrl,
    "aws.sqs.waitTime" -> "1",
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "calm-transformer"
  )

  it(
    "should poll the dynamo stream for calm data, transform it into unified items and push them into the id_minter SNS topic") {
    val calmTransformable = CalmTransformable(RecordID = "RecordID1",
      RecordType = "Collection",
      AltRefNo = "AltRefNo1",
      RefNo = "RefNo1",
      data = """{"AccessStatus": ["public"]}""")
    val sqsMessage = SQSMessage(Some("subject"),
      JsonUtil.toJson(calmTransformable).get,
      "topic",
      "messageType",
      "timestamp")
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size 1
      assertSNSMessageContainsCalmDataWith(snsMessages.head, Some("public"))
    }

    val calmTransformable2 = CalmTransformable(RecordID = "RecordID2",
      RecordType = "Collection",
      AltRefNo = "AltRefNo2",
      RefNo = "RefNo2",
      data = """{"AccessStatus": ["restricted"]}""")
    val sqsMessage2 = SQSMessage(Some("subject"),
      JsonUtil.toJson(calmTransformable2).get,
      "topic",
      "messageType",
      "timestamp")
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage2).get)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size 2

      assertSNSMessageContainsCalmDataWith(snsMessages.head, Some("public"))
      assertSNSMessageContainsCalmDataWith(snsMessages.tail.head,
                                           Some("restricted"))
    }
  }

  private def assertSNSMessageContainsCalmDataWith(
    snsMessage: MessageInfo,
    AccessStatus: Option[String]): Any = {
    //currently for calm data we only output hardcoded sample values
    snsMessage.message shouldBe JsonUtil
      .toJson(
        Work(
          identifiers = List(SourceIdentifier("source", "key", "value")),
          label = "calm data label"
        ))
      .get
  }
}
