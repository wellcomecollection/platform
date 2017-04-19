package uk.ac.wellcome.transformer

import com.gu.scanamo.Scanamo
import com.twitter.inject.Injector
import com.twitter.inject.app.TestInjector
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.{MiroTransformable, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.transformer.modules.{KinesisWorker, StreamsRecordProcessorFactoryModule}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.modules.{AmazonCloudWatchModule, TransformableParserModule}
import uk.ac.wellcome.transformer.utils.TransformerIntegrationTest
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerIntegrationTest extends TransformerIntegrationTest {

  override def injector: Injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "eu-west-1",
        "aws.dynamo.streams.appName" -> "test-transformer-miro",
        "aws.dynamo.streams.arn" -> miroDataStreamArn,
        "aws.dynamo.tableName" -> miroDataTableName,
        "aws.sns.topic.arn" -> idMinterTopicArn
      ),
      modules = Seq(
        StreamsRecordProcessorFactoryModule,
        DynamoConfigModule,
        AkkaModule,
        TransformableParserModule,
        SNSConfigModule,
        AmazonCloudWatchModule,
        LocalKinesisClientLibConfigurationModule,
        LocalSNSClient,
        DynamoDBLocalClientModule,
        LocalKinesisModule
      )
    )

  test("it should poll the dynamo stream for miro data, transform it into unified items and push them into the id_minter SNS topic") {
    val miroId = "1234"
    val imageTitle = "some image title"
    putMiroImageInDynamoDb(miroId, imageTitle)

    KinesisWorker.singletonStartup(injector)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)
      assertSNSMessageContains(snsMessages.head, miroId, imageTitle)
    }

    val secondMiroId = "5678"
    val secondImageTitle = "some other image title"
    putMiroImageInDynamoDb(secondMiroId, secondImageTitle)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (2)

      assertSNSMessageContains(snsMessages.head, miroId, imageTitle)
      assertSNSMessageContains(snsMessages.tail.head,
                               secondMiroId,
                               secondImageTitle)
    }
  }

  private def assertSNSMessageContains(snsMessage: MessageInfo,
                                       miroId: String,
                                       imageTitle: String) = {
    snsMessage.message shouldBe JsonUtil
      .toJson(
        UnifiedItem(identifiers = List(SourceIdentifier("Miro", "MiroID", miroId)),
                    title = Some(imageTitle)))
      .get
  }

  private def putMiroImageInDynamoDb(miroId: String, imageTitle: String) = {
    Scanamo.put(dynamoDbClient)(miroDataTableName)(
      MiroTransformable(miroId,
                        "Images-A",
                        s"""{"image_title": "$imageTitle"}"""))
  }

}
