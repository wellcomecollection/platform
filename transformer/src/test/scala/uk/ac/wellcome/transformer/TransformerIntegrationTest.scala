package uk.ac.wellcome.transformer

import com.amazonaws.services.dynamodbv2.model.{DescribeStreamRequest, GetRecordsRequest, GetShardIteratorRequest, ShardIteratorType}
import com.gu.scanamo.Scanamo
import com.twitter.inject.Injector
import com.twitter.inject.app.TestInjector
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.{MiroTransformable, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.transformer.modules.{KinesisClientLibConfigurationModule, KinesisWorker, StreamsRecordProcessorFactoryModule}
import uk.ac.wellcome.test.utils.IntegrationTestBase
import uk.ac.wellcome.transformer.modules.TransformableParserModule
import uk.ac.wellcome.utils.JsonUtil

class TransformerIntegrationTest
    extends IntegrationTestBase
    with Eventually
    with IntegrationPatience {
  override def injector: Injector =
    TestInjector(
      flags = Map(
        "aws.region"-> "eu-west-1",
        "aws.dynamo.streams.appName"->s"transformer-${Math.random()}",
        "aws.dynamo.streams.arn"-> miroDataStreamArn,
        "aws.dynamo.tableName" -> miroDataTableName,
        "aws.sns.topic.arn" -> idMinterTopicArn,
        "source" -> "Miro"
      ),
      modules = Seq(
        StreamsRecordProcessorFactoryModule,
        KinesisClientLibConfigurationModule,
        DynamoConfigModule,
        AkkaModule,
        TransformableParserModule,
        SNSConfigModule,
        LocalSNSClient,
        DynamoDBLocalClientModule,
        LocalKinesisModule,
        DummyCloudWatchClientModule
      ))

  test("it should transform Miro data into Unified items and push them into the id_minter SNS topic") {
    val miroId = "1234"
    val imageTitle = "some image title"

    Scanamo.put(dynamoDbClient)(miroDataTableName)(
      MiroTransformable(miroId,
        "Images-A",
        s"""{"image_title": "$imageTitle"}"""))

    KinesisWorker.singletonStartup(injector)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)
      snsMessages.head.message shouldBe JsonUtil.toJson(
        UnifiedItem(List(SourceIdentifier("Miro", "MiroID", miroId)),
                    Some(imageTitle),
                    None)).get
    }

  }

}
