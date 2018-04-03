package uk.ac.wellcome.test.fixtures

import scala.util.{Random, Try}
import uk.ac.wellcome.models.{Id, Versioned}
import uk.ac.wellcome.test.utils.DynamoDBLocalClients
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey

import scala.collection.JavaConversions._

trait LocalDynamoDb[T <: Versioned with Id] extends DynamoDBLocalClients {

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port

  private val accessKey = "access"
  private val secretKey = "secret"

  def dynamoDbLocalEndpointFlags(tableName: String) =
    Map(
      "aws.region" -> "localhost",
      "aws.dynamo.tableName" -> tableName,
      "aws.dynamoDb.endpoint" -> dynamoDBEndPoint,
      "aws.dynamoDb.accessKey" -> accessKey,
      "aws.dynamoDb.secretKey" -> secretKey
    )

  private val credentials = new AWSStaticCredentialsProvider(
    new BasicAWSCredentials(accessKey, secretKey))

  val dynamoDbClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(credentials)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()

  implicit val evidence: DynamoFormat[T]

  case class FixtureParams(tableName: String, indexName: String)

  def withLocalDynamoDbTableAndIndex[R](
    testWith: TestWith[FixtureParams, R]): R = {
    val tableName = Random.alphanumeric.take(10).mkString
    val indexName = Random.alphanumeric.take(10).mkString

    try {
      createTable(tableName, indexName)
      testWith(FixtureParams(tableName, indexName))
    } finally {
      deleteAllTables()
    }
  }

  def withLocalDynamoDbTable[R](testWith: TestWith[String, R]): R = {
    withLocalDynamoDbTableAndIndex { fixtures =>
      testWith(fixtures.tableName)
    }
  }

  private def createTable(tableName: String, indexName: String) = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(tableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("id")
          .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("id")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("reindexShard")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("reindexVersion")
            .withAttributeType("N")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName(indexName)
            .withProjection(
              new Projection().withProjectionType(ProjectionType.ALL))
            .withKeySchema(
              new KeySchemaElement()
                .withAttributeName("reindexShard")
                .withKeyType(KeyType.HASH),
              new KeySchemaElement()
                .withAttributeName("reindexVersion")
                .withKeyType(KeyType.RANGE)
            )
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))))
  }

  private def deleteAllTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(dynamoDbClient.deleteTable)
  }

}
