package uk.ac.wellcome.transformer

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.gu.scanamo.Scanamo
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{MiroTransformable, Work}
import uk.ac.wellcome.test.utils.MessageInfo
import uk.ac.wellcome.transformer.utils.TransformerFeatureTest
import uk.ac.wellcome.utils.JsonUtil

class MiroTransformerFeatureTest
    extends FunSpec
    with TransformerFeatureTest
    with Matchers {

  private val appName = "test-transformer-miro"
  override val flags: Map[String, String] = Map(
    "aws.region" -> "eu-west-1",
    "aws.dynamo.miroData.streams.appName" -> appName,
    "aws.dynamo.miroData.streams.arn" -> miroDataStreamArn,
    "aws.dynamo.miroData.tableName" -> miroDataTableName,
    "aws.sns.topic.arn" -> idMinterTopicArn,
    "aws.metrics.namespace" -> "miro-transformer"
  )
  override val kinesisClientLibConfiguration: KinesisClientLibConfiguration =
    kinesisClientLibConfiguration(appName, miroDataStreamArn)

  it(
    "should poll the Dynamo stream for Miro records, transform into Work instances, and push them into the id_minter SNS topic") {
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
                        s"""{
          "image_title": "$imageTitle",
          "image_cleared": "Y",
          "image_copyright_cleared": "Y"
        }"""))
  }

}
