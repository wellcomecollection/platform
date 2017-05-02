package uk.ac.wellcome.transformer

import com.gu.scanamo.Scanamo
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.FunSpec
import uk.ac.wellcome.models.{MiroTransformable, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest extends FunSpec with TransformerFeatureTest {

  override val server = new EmbeddedHttpServer(
    transformerServer,
    flags = Map(
      "aws.region" -> "eu-west-1",
      "aws.dynamo.streams.appName" -> "test-transformer-miro",
      "aws.dynamo.streams.arn" -> miroDataStreamArn,
      "aws.dynamo.tableName" -> miroDataTableName,
      "aws.sns.topic.arn" -> idMinterTopicArn
    )
  )

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
        UnifiedItem(identifiers =
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
