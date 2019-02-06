package uk.ac.wellcome.platform.archive.common.progress.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import scala.collection.JavaConverters._
import uk.ac.wellcome.fixtures.TestWith

import scala.util.Random

trait LocalProgressTrackerDynamoDb extends LocalDynamoDb {
  private def createProgressTrackerTable(
    dynamoDbClient: AmazonDynamoDB): Table = {
    val tableName = Random.alphanumeric.take(10).mkString
    val tableIndex = Random.alphanumeric.take(10).mkString
    val table = Table(tableName, tableIndex)

    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(
          new KeySchemaElement()
            .withAttributeName("id")
            .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("id")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("bagIdIndex")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("createdDate")
            .withAttributeType("S")
        )
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName(table.index)
            .withProjection(
              new Projection().withProjectionType(ProjectionType.INCLUDE)
                .withNonKeyAttributes(List("bagIdIndex", "id", "createdDate").asJava)
            ).withKeySchema(
              new KeySchemaElement()
                .withAttributeName("bagIdIndex")
                .withKeyType(KeyType.HASH),
            new KeySchemaElement()
              .withAttributeName("createdDate")
              .withKeyType(KeyType.RANGE)
            ).withProvisionedThroughput(
            new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
        ).withProvisionedThroughput(
          new ProvisionedThroughput()
            .withReadCapacityUnits(1L)
            .withWriteCapacityUnits(1L))
    )
    eventually {
      waitUntilActive(dynamoDbClient, table.name)
    }
    table
  }

  def withProgressTrackerTable[R](testWith: TestWith[Table, R]): R =
    withSpecifiedLocalDynamoDbTable(createProgressTrackerTable) { table =>
      testWith(table)
    }
}
