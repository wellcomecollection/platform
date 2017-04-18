package uk.ac.wellcome.transformer

import com.gu.scanamo.Scanamo
import com.twitter.inject.Injector
import com.twitter.inject.app.TestInjector
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.{
  CalmTransformable,
  MiroTransformable,
  SourceIdentifier,
  UnifiedItem
}
import uk.ac.wellcome.platform.transformer.modules.{
  KinesisClientLibConfigurationModule,
  KinesisWorker,
  StreamsRecordProcessorFactoryModule
}
import uk.ac.wellcome.test.utils.{IntegrationTestBase, MessageInfo}
import uk.ac.wellcome.transformer.modules.{
  AmazonCloudWatchModule,
  TransformableParserModule
}
import uk.ac.wellcome.utils.JsonUtil

class CalmTransformerIntegrationTest
    extends IntegrationTestBase
    with Eventually
    with IntegrationPatience {

  override def injector: Injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.dynamo.streams.appName" -> s"transformer-calm",
        "aws.dynamo.streams.arn" -> calmDataStreamArn,
        "aws.dynamo.tableName" -> calmDataTableName,
        "aws.sns.topic.arn" -> idMinterTopicArn
      ),
      modules = Seq(
        StreamsRecordProcessorFactoryModule,
        KinesisClientLibConfigurationModule,
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
      CalmTransformable("RecordID1",
                        "Collection",
                        "AltRefNo1",
                        "RefNo1",
                        """{"AccessStatus": ["public"]}"""))

    KinesisWorker.singletonStartup(injector)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)
      assertSNSMessageContainsCalmDataWith(snsMessages.head, Some("public"))
    }

    Scanamo.put(dynamoDbClient)(calmDataTableName)(
      CalmTransformable("RecordID2",
                        "Collection",
                        "AltRefNo2",
                        "RefNo2",
                        """{"AccessStatus": ["secret"]}"""))

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (2)

      assertSNSMessageContainsCalmDataWith(snsMessages.head, Some("public"))
      assertSNSMessageContainsCalmDataWith(snsMessages.tail.head,
                                           Some("secret"))
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
