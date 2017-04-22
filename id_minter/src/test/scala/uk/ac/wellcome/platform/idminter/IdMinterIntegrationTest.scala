package uk.ac.wellcome.platform.idminter

import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{IdentifiedUnifiedItem, Identifier, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.idminter.modules.IdMinterModule
import uk.ac.wellcome.test.utils.{DynamoDBLocal, SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

class IdMinterIntegrationTest
    extends IntegrationTest
    with SQSLocal
    with DynamoDBLocal
    with SNSLocal
    with Eventually
    with IntegrationPatience {

  override def topicName: String = "ingester"
  override def queueName: String = "id_minter"

  override val injector =
    TestInjector(
      flags = Map(
        "aws.region" -> "local",
        "aws.sqs.queue.url" -> queueUrl,
        "aws.sqs.waitTime" -> "1",
        "aws.sns.topic.arn" -> topicArn,
        "aws.dynamo.tableName" -> identifiersTableName
      ),
      modules = Seq(AkkaModule,
                    LocalSNSClient,
                    DynamoDBLocalClientModule,
                    SQSReaderModule,
                    SQSLocalClientModule,
                    SNSConfigModule,
                    SQSConfigModule,
                    DynamoConfigModule)
    )

  test("it should read a unified item from the SQS queue, generate a canonical id, save it in dynamoDB and send a message to the SNS topic with the original unified item and the id") {
    val unifiedItem =
      UnifiedItem(identifiers =
                    List(SourceIdentifier("Miro", "MiroID", "1234")),
                  accessStatus = Option("super-secret"))
    val sqsMessage = SQSMessage(Some("subject"),
                                UnifiedItem.json(unifiedItem),
                                "topic",
                                "messageType",
                                "timestamp")

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)

    IdMinterModule.singletonStartup(injector)

    eventually {
      val dynamoIdentifiersRecords =
        Scanamo.queryIndex[Identifier](dynamoDbClient)(
          "Identifiers",
          "MiroID")('MiroID -> "1234")
      dynamoIdentifiersRecords should have size (1)
      val id = extractId(dynamoIdentifiersRecords)
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)
      JsonUtil
        .fromJson[IdentifiedUnifiedItem](messages.head.message)
        .get shouldBe IdentifiedUnifiedItem(id.CanonicalID, unifiedItem)
      messages.head.subject should be("identified-item")
    }
  }

  test("it should keep polling the SQS queue for new messages") {
    val firstMiroId = "1234"
    val sqsMessage = generateSqsMessage(firstMiroId)

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)

    IdMinterModule.singletonStartup(injector)

    eventually {
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> firstMiroId) should have size (1)
    }

    val secondMiroId = "5678"
    val secondSqsMessage = generateSqsMessage(secondMiroId)
    sqsClient.sendMessage(queueUrl,
                          JsonUtil.toJson(secondSqsMessage).get)

    eventually {
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> secondMiroId) should have size (1)
      Scanamo.scan[Identifier](dynamoDbClient)("Identifiers") should have size (2)
    }
  }

  test("it should keep polling if something fails processing a message") {
    sqsClient.sendMessage(queueUrl, "not a json string")

    IdMinterModule.singletonStartup(injector)

    val miroId = "1234"
    val sqsMessage = generateSqsMessage(miroId)

    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(sqsMessage).get)
    eventually {
      Scanamo.queryIndex[Identifier](dynamoDbClient)("Identifiers", "MiroID")(
        'MiroID -> miroId) should have size (1)
    }

  }

  private def generateSqsMessage(MiroID: String) = {
    val unifiedItem = UnifiedItem(
      identifiers = List(SourceIdentifier("Miro", "MiroID", MiroID)),
      accessStatus = Option("super-secret"))
    SQSMessage(Some("subject"),
               UnifiedItem.json(unifiedItem),
               "topic",
               "messageType",
               "timestamp")
  }

  private def extractId(
    dynamoIdentifiersRecords: List[Either[DynamoReadError, Identifier]]) = {
    dynamoIdentifiersRecords.head
      .asInstanceOf[Right[DynamoReadError, Identifier]]
      .b
  }
}
