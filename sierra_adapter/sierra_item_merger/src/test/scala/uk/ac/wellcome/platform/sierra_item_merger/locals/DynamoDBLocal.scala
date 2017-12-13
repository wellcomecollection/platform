package uk.ac.wellcome.platform.sierra_item_merger.locals

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.test.utils.DynamoDBLocalClients

import scala.collection.JavaConversions._

trait DynamoDBLocal extends BeforeAndAfterEach with DynamoDBLocalClients {
  this: Suite =>

  val tableName = "sierraItemsTable"

  deleteTable()

  private val table: CreateTableResult = createTable()

  val streamArn: String =
    table.getTableDescription.getLatestStreamArn

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearTable()
  }

  private def clearTable(): List[DeleteItemResult] =
    Scanamo.scan[MergedSierraRecord](dynamoDbClient)(tableName).map {
      case Right(item) =>
        dynamoDbClient.deleteItem(
          tableName,
          Map("id" -> new AttributeValue(item.id))
        )
      case a =>
        throw new Exception(s"Unable to clear the table $tableName error $a")
    }

  private def deleteTable(): Unit = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(dynamoDbClient.deleteTable)
  }

  private def createTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(tableName)
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
          .withWriteCapacityUnits(1L)))
  }
}
