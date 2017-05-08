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

  it("should poll the Dynamo stream for Miro records, transform into Work instances, and push them into the id_minter SNS topic") {
    val miroID = "M0000001"
    val label = "A guide for a giraffe"
    putMiroImageInDynamoDb(miroID, label)

    eventually {
      val snsMessages = listMessagesReceivedFromSNS()
      snsMessages should have size (1)
      assertSNSMessageContains(snsMessages.head, miroID, label)
    }

    val secondMiroID = "M0000002"
    val secondLabel = "A song about a snake"
    putMiroImageInDynamoDb(secondMiroID, secondLabel)

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

  private def putMiroImageInDynamoDb(miroID: String, imageTitle: String) = {
    Scanamo.put(dynamoDbClient)(miroDataTableName)(
      MiroTransformable(miroID,
                        "Images-A",
                        s"""{"image_title": "$imageTitle"}"""))
  }

}
