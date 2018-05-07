package uk.ac.wellcome.storage.test.fixtures

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import org.scalatest.concurrent.Eventually

import scala.util.Random
import uk.ac.wellcome.models.{Id, Versioned}
import uk.ac.wellcome.test.fixtures._
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.collection.JavaConversions._

object LocalDynamoDb {
  case class Table(name: String, index: String)
}

trait LocalDynamoDb[T <: Versioned with Id]
  extends Eventually
    with ExtendedPatience {

  import LocalDynamoDb._

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port

  private val accessKey = "access"
  private val secretKey = "secret"

  def dynamoDbLocalEndpointFlags(table: Table) = dynamoClientLocalFlags ++ Map(
    "aws.dynamo.tableName" -> table.name
  )

  def dynamoClientLocalFlags = Map(
    "aws.dynamoDb.endpoint" -> dynamoDBEndPoint,
    "aws.dynamoDb.accessKey" -> accessKey,
    "aws.dynamoDb.secretKey" -> secretKey,
    "aws.region" -> "localhost"
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
            .withIndexName(table.index)
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

    eventually {
      waitUntilActive(dynamoDbClient, table.name)
    }

    table
  }

  private def deleteAllTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(dynamoDbClient.deleteTable)
  }

}
