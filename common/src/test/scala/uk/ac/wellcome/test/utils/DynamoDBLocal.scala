package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder, AmazonDynamoDBStreamsClientBuilder}
import com.google.inject.{Provides, Singleton}
import com.gu.scanamo.Scanamo
import com.twitter.inject.TwitterModule
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.Identifier

import scala.collection.JavaConversions._

trait DynamoDBLocal
    extends Suite
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port

  private val dynamoDBLocalCredentialsProvider =
    new AWSStaticCredentialsProvider(
      new BasicAWSCredentials("access", "secret"))

  protected val dynamoDbClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(dynamoDBLocalCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()

  protected val identifiersTableName = "Identifiers"
  protected val miroDataTableName = "MiroData"
  protected val calmDataTableName = "CalmData"

  deleteTables()
  private val identifiersTable = createIdentifiersTable()
  private val miroDataTable = createMiroDataTable()
  private val calmDataTable = createCalmDataTable()

  protected val miroDataStreamArn =
    miroDataTable.getTableDescription.getLatestStreamArn
  protected val calmDataStreamArn =
    calmDataTable.getTableDescription.getLatestStreamArn

  protected val streamsClient = AmazonDynamoDBStreamsClientBuilder
    .standard()
    .withCredentials(dynamoDBLocalCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(tableName => clearTable(tableName))
  }

  private def clearTable(tableName: String): List[DeleteItemResult] =
    Scanamo.scan[Identifier](dynamoDbClient)(tableName).map {
      case Right(id) =>
        dynamoDbClient.deleteItem(
          tableName,
          Map("CanonicalID" -> new AttributeValue(id.CanonicalID)))
      case _ => throw new Exception("Unable to clear the table")
    }

  private def deleteTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(tableName => dynamoDbClient.deleteTable(tableName))
  }

  //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
  private def createCalmDataTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(calmDataTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("RecordID")
          .withKeyType(KeyType.HASH))
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("RecordType")
          .withKeyType(KeyType.RANGE))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("RecordID")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("RecordType")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("RefNo")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("AltRefNo")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
        .withStreamSpecification(new StreamSpecification()
          .withStreamEnabled(true)
          .withStreamViewType(StreamViewType.NEW_IMAGE))
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName("RefNo")
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
            .withProjection(
              new Projection().withProjectionType(ProjectionType.ALL))
            .withKeySchema(new KeySchemaElement()
              .withAttributeName("RefNo")
              .withKeyType(KeyType.HASH)),
          new GlobalSecondaryIndex()
            .withIndexName("AltRefNo")
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
            .withProjection(
              new Projection().withProjectionType(ProjectionType.ALL))
            .withKeySchema(new KeySchemaElement()
              .withAttributeName("AltRefNo")
              .withKeyType(KeyType.HASH))
        )
    )
  }

  //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
  private def createMiroDataTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(miroDataTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("MiroID")
          .withKeyType(KeyType.HASH))
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("MiroCollection")
          .withKeyType(KeyType.RANGE))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("MiroID")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("MiroCollection")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
        .withStreamSpecification(new StreamSpecification()
          .withStreamEnabled(true)
          .withStreamViewType(StreamViewType.NEW_IMAGE))
    )
  }

  //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
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

  object DynamoDBLocalClientModule extends TwitterModule {

    @Singleton
    @Provides
    def providesDynamoDbClient: AmazonDynamoDB = dynamoDbClient
  }
}
