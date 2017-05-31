package uk.ac.wellcome.transformer

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.gu.scanamo.Scanamo
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{CalmTransformable, SourceIdentifier, Work}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class CalmTransformerFeatureTest
    extends FunSpec
    with TransformerFeatureTest
    with Matchers {

  private val appName = "test-transformer-calm"

  override val flags: Map[String, String] = Map(
    "aws.region" -> "eu-west-1",
    "aws.dynamo.calmData.streams.appName" -> appName,
    "aws.dynamo.calmData.streams.arn" -> calmDataStreamArn,
    "aws.dynamo.calmData.tableName" -> calmDataTableName,
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "calm-transformer"
  )
  override val kinesisClientLibConfiguration: KinesisClientLibConfiguration =
    kinesisClientLibConfiguration(appName, calmDataStreamArn)

  it("should poll the dynamo stream for calm data, transform it into unified items and push them into the id_minter SNS topic") {
    Scanamo.put(dynamoDbClient)(calmDataTableName)(
      CalmTransformable(RecordID = "RecordID1",
                        RecordType = "Collection",
                        AltRefNo = "AltRefNo1",
                        RefNo = "RefNo1",
                        data = """{"AccessStatus": ["public"]}"""))

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)
      assertSNSMessageContainsCalmDataWith(snsMessages.head, Some("public"))
    }

    Scanamo.put(dynamoDbClient)(calmDataTableName)(
      CalmTransformable(RecordID = "RecordID2",
                        RecordType = "Collection",
                        AltRefNo = "AltRefNo2",
                        RefNo = "RefNo2",
                        data = """{"AccessStatus": ["restricted"]}"""))

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (2)

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
