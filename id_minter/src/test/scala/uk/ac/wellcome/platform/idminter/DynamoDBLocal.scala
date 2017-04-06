package uk.ac.wellcome.platform.idminter

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import uk.ac.wellcome.platform.idminter.modules.Id

import scala.collection.JavaConversions._

trait DynamoDBLocal extends Suite with BeforeAndAfterEach with BeforeAndAfterAll{

  val port = 45678
  val dynamoDbClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("access", "secret")))
    .withEndpointConfiguration(new EndpointConfiguration("http://localhost:" + port, "localhost")).build()

  def deleteTable() = {
    if(!dynamoDbClient.listTables().getTableNames.isEmpty)
      dynamoDbClient.deleteTable("Identifiers")
  }

  override def beforeAll(): Unit = {
    deleteTable()
    createTable()
  }

  override def beforeEach(): Unit = {
    clearTable()
  }

  private def clearTable() = Scanamo.scan[Id](dynamoDbClient)("Identifiers").map {
    case Right(id) => dynamoDbClient.deleteItem("Identifiers", Map("CanonicalID"-> new AttributeValue(id.CanonicalID)))
    case _ => throw new Exception("Unable to clear the table")
  }

  private def createTable(): Unit ={
    //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName("Identifiers")
        .withKeySchema(
          new KeySchemaElement()
            .withAttributeName("CanonicalID")
            .withKeyType(KeyType.HASH))
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName("MiroID")
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
            .withProjection(new Projection()
              .withProjectionType(ProjectionType.ALL))
            .withKeySchema(new KeySchemaElement()
              .withAttributeName("MiroID")
              .withKeyType(KeyType.HASH)))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("CanonicalID")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("MiroID")
            .withAttributeType("S"))
        .withProvisionedThroughput(
          new ProvisionedThroughput()
            .withReadCapacityUnits(1L)
            .withWriteCapacityUnits(1L)))
  }
}
