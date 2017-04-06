package uk.ac.wellcome.platform.idminter

import javax.inject.Singleton

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Provides
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.TwitterModule
import com.twitter.inject.server.FeatureTest
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{Identifier, UnifiedItem}
import uk.ac.wellcome.platform.idminter.modules.Id
import uk.ac.wellcome.utils.JsonUtil
import scala.collection.JavaConversions._

class FunctionalTest extends FeatureTest with Eventually with IntegrationPatience with SQSLocal with DynamoDBLocal with SNSLocal {

  val server = new EmbeddedHttpServer(new Server{
    override val modules = Seq(DynamoDBLocalClientModule, LocalSNSClient)
  })
  amazonSNS.subscribe(ingestTopicArn, "http", ingesterQueueUrl)

  test("it should read a unified item from the SQS queue, generate a canonical id, save it in dynamoDB and send a message to the SNS topic with the original unified item and the id") {
    val unifiedItem = UnifiedItem("id", List(Identifier("Miro", "MiroId", "1234")), Option("super-secret"))
    val sqsMessage = SQSMessage(Some("subject"),UnifiedItem.json(unifiedItem), "topic", "messageType", "timestamp")

    sqsClient.sendMessage(idMinterQueueUrl, JsonUtil.toJson(sqsMessage).get)

    eventually {
      val dynamoIdentifiersRecords = Scanamo.queryIndex[Id](dynamoDbClient)("Identifiers", "MiroID")('MiroID -> "1234")
      dynamoIdentifiersRecords should have size(1)
      dynamoIdentifiersRecords.head shouldBe a[Right[DynamoReadError,Id]]

      sqsClient.receiveMessage(ingesterQueueUrl).getMessages should have size (1)

    }
  }

  object LocalSNSClient extends TwitterModule {

    @Singleton
    @Provides
    def providesSNSClient: AmazonSNS = amazonSNS
  }

  object DynamoDBLocalClientModule extends TwitterModule  {

    @Singleton
    @Provides
    def providesDynamoDbClient: AmazonDynamoDB = dynamoDbClient
  }
}
