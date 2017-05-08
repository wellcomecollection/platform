package uk.ac.wellcome.transformer

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.sns.AmazonSNS
import com.gu.scanamo.Scanamo
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.FunSpec
import uk.ac.wellcome.models.{MiroTransformable, SourceIdentifier, Work}
import uk.ac.wellcome.platform.transformer.Server
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest extends FunSpec with TransformerFeatureTest {

  private val appName = "test-transformer-miro"
  override val server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.region" -> "eu-west-1",
      "aws.dynamo.streams.appName" -> appName,
      "aws.dynamo.streams.arn" -> miroDataStreamArn,
      "aws.dynamo.tableName" -> miroDataTableName,
      "aws.sns.topic.arn" -> idMinterTopicArn
    )
  )
    .bind[AmazonSNS](amazonSNS)
    .bind[AmazonDynamoDB](dynamoDbClient)
    .bind[AmazonKinesis](new AmazonDynamoDBStreamsAdapterClient(streamsClient))
    .bind[KinesisClientLibConfiguration](kinesisClientLibConfiguration(appName, miroDataStreamArn))

  it("should poll the dynamo stream for miro data, transform it into unified items and push them into the id_minter SNS topic") {
    val miroId = "1234"
    val imageTitle = "some image title"
    putMiroImageInDynamoDb(miroId, imageTitle)

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
        Work(identifiers =
                      List(SourceIdentifier("Miro", "MiroID", miroId)),
                    label = imageTitle))
      .get
  }

  private def putMiroImageInDynamoDb(miroId: String, imageTitle: String) = {
    Scanamo.put(dynamoDbClient)(miroDataTableName)(
      MiroTransformable(miroId,
                        "Images-A",
                        s"""{"image_title": "$imageTitle"}"""))
  }

}
