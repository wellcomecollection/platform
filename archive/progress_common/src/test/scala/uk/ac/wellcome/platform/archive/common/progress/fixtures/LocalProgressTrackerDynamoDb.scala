package uk.ac.wellcome.platform.archive.common.progress.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table

import scala.util.Random

trait LocalProgressTrackerDynamoDb extends LocalDynamoDb {
  def createTable(table: LocalDynamoDb.Table): Table = Table("table", "index")

  def createProgressTrackerTable(
    dynamoDbClient: AmazonDynamoDB): LocalDynamoDb.Table = {
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
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
    )
    eventually {
      waitUntilActive(dynamoDbClient, table.name)
    }
    table
  }
}
