package uk.ac.wellcome.platform.matcher.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table

import scala.util.Random

trait LocalLockTableDynamoDb extends LocalDynamoDb {
  def createTable(table: LocalDynamoDb.Table): Table = Table("table", "index")

  def createLockTable(dynamoDbClient: AmazonDynamoDB): LocalDynamoDb.Table = {
    val tableName = Random.alphanumeric.take(10).mkString

    val table = Table(tableName, "")

    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(new KeySchemaElement()
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

  def createThingTable(dynamoDbClient: AmazonDynamoDB): LocalDynamoDb.Table = {
    val tableName = Random.alphanumeric.take(10).mkString

    val table = Table(tableName, "")

    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(new KeySchemaElement()
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
