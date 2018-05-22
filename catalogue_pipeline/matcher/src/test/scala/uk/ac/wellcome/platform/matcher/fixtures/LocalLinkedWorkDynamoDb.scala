package uk.ac.wellcome.platform.matcher.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.collection.JavaConverters._
import scala.util.Random

trait LocalLinkedWorkDynamoDb
  extends Eventually
    with ExtendedPatience
{

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port

  private val regionName = "localhost"

  private val accessKey = "access"
  private val secretKey = "secret"

  def dynamoDbLocalEndpointFlags(table: Table) = dynamoClientLocalFlags ++ Map(
    "aws.dynamo.tableName" -> table.name
  )

  def dynamoClientLocalFlags = Map(
    "aws.dynamoDb.endpoint" -> dynamoDBEndPoint,
    "aws.dynamoDb.accessKey" -> accessKey,
    "aws.dynamoDb.secretKey" -> secretKey,
    "aws.dynamoDb.region" -> regionName
  )

  val dynamoDbClient: AmazonDynamoDB = DynamoClientFactory.create(
    region = regionName,
    endpoint = dynamoDBEndPoint,
    accessKey = accessKey,
    secretKey = secretKey
  )

  def withLocalDynamoDbTable[R] = fixture[Table, R](
    create = {
      val tableName = Random.alphanumeric.take(10).mkString
      val indexName = Random.alphanumeric.take(10).mkString

      createTable(Table(tableName, indexName))
    },
    destroy = { _ =>
      deleteAllTables()
    }
  )

  private def createTable(table: Table): Table = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("workId")
          .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("workId")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("setId")
            .withAttributeType("S"),
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName(table.index)
            .withProjection(
              new Projection().withProjectionType(ProjectionType.ALL))
            .withKeySchema(
              new KeySchemaElement()
                .withAttributeName("setId")
                .withKeyType(KeyType.HASH)
            )
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))))
    eventually {
      waitUntilActive(dynamoDbClient, table.name)
    }
    table
  }

  private def deleteAllTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .asScala
      .foreach(dynamoDbClient.deleteTable)
  }
}
