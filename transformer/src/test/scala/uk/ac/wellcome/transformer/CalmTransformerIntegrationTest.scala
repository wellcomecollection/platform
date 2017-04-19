package uk.ac.wellcome.transformer

import com.gu.scanamo.Scanamo
import com.twitter.inject.Injector
import com.twitter.inject.app.TestInjector
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.{CalmTransformable, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.transformer.modules.{KinesisWorker, StreamsRecordProcessorFactoryModule}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.modules.{AmazonCloudWatchModule, TransformableParserModule}
import uk.ac.wellcome.transformer.utils.TransformerIntegrationTest
import uk.ac.wellcome.utils.JsonUtil

class CalmTransformerIntegrationTest extends TransformerIntegrationTest {

  override def injector: Injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.dynamo.streams.appName" -> "test-transformer-calm",
        "aws.dynamo.streams.arn" -> calmDataStreamArn,
        "aws.dynamo.tableName" -> calmDataTableName,
        "aws.sns.topic.arn" -> idMinterTopicArn
      ),
      modules = Seq(
        StreamsRecordProcessorFactoryModule,
        LocalKinesisClientLibConfigurationModule,
        DynamoConfigModule,
        AkkaModule,
        TransformableParserModule,
        SNSConfigModule,
        AmazonCloudWatchModule,
        LocalSNSClient,
        DynamoDBLocalClientModule,
        LocalKinesisModule
      )
    )

  test("it should poll the dynamo stream for calm data, transform it into unified items and push them into the id_minter SNS topic") {
    Scanamo.put(dynamoDbClient)(calmDataTableName)(
      CalmTransformable(RecordID = "RecordID1",
        RecordType = "Collection",
        AltRefNo = "AltRefNo1",
        RefNo = "RefNo1",
        data = """{"AccessStatus": ["public"]}"""))

    KinesisWorker.singletonStartup(injector)

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
        UnifiedItem(
          List(SourceIdentifier("source", "key", "value")),
          None,
          accessStatus = AccessStatus
        ))
      .get
  }
}
