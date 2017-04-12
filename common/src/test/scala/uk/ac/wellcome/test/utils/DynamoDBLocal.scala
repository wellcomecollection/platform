package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{
  AmazonDynamoDB,
  AmazonDynamoDBClientBuilder
}
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.Identifier

import scala.collection.JavaConversions._

trait DynamoDBLocal
    extends Suite
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val port = 45678
  val dynamoDbClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(new AWSStaticCredentialsProvider(
      new BasicAWSCredentials("access", "secret")))
    .withEndpointConfiguration(
      new EndpointConfiguration("http://localhost:" + port, "localhost"))
    .build()

  protected val identifiersTableName = "Identifiers"
  protected val miroDataTableName = "MiroData"

  override def beforeAll(): Unit = {
    super.beforeAll()
    deleteTable()
    createTables()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearTable()
  }

  private def clearTable() =
    Scanamo.scan[Identifier](dynamoDbClient)(identifiersTableName).map {
      case Right(id) =>
        dynamoDbClient.deleteItem(
          identifiersTableName,
          Map("CanonicalID" -> new AttributeValue(id.CanonicalID)))
      case _ => throw new Exception("Unable to clear the table")
    }

  private def deleteTable() = {
    if (!dynamoDbClient.listTables().getTableNames.isEmpty)
      dynamoDbClient.deleteTable(identifiersTableName)
  }


  private def createTables(): Unit = {
    //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
    createIdentifiersTable()
    createMiroDataTable()
  }

  private def createMiroDataTable(): Unit = {
    dynamoDbClient.createTable(new CreateTableRequest().withTableName(miroDataTableName)
      .withKeySchema(new KeySchemaElement().withAttributeName("MiroID").withKeyType(KeyType.HASH))
      .withKeySchema(new KeySchemaElement().withAttributeName("MiroCollection").withKeyType(KeyType.RANGE))
      .withAttributeDefinitions(
        new AttributeDefinition().withAttributeName("MiroID").withAttributeType("S"),
        new AttributeDefinition().withAttributeName("MiroCollection").withAttributeType("S"))  )
  }

  private def createIdentifiersTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(identifiersTableName)
        .withKeySchema(new KeySchemaElement()
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
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L)))
  }
}
