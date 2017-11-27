package uk.ac.wellcome.platform.sierra_to_dynamo.locals

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord
import uk.ac.wellcome.platform.sierra_to_dynamo.models.SierraRecord._
import uk.ac.wellcome.test.utils.DynamoDBLocalClients

import scala.collection.JavaConversions._

trait SierraDynamoDBLocal
    extends BeforeAndAfterEach
    with DynamoDBLocalClients { this: Suite =>

  val tableName = "SierraData"

  deleteTables()
  createTable()

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearTable()
  }

  private def clearTable(): List[DeleteItemResult] =
    Scanamo.scan[SierraRecord](dynamoDbClient)(tableName).map {
      case Right(record) =>
        dynamoDbClient.deleteItem(
          tableName,
          Map("id" -> new AttributeValue(record.id))
        )
      case e =>
        throw new Exception(s"Unable to clear the table $tableName error $e")
    }

  private def deleteTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(tableName => dynamoDbClient.deleteTable(tableName))
  }
  // TODO delete and use terraform apply once this issue is fixed:
  // https://github.com/terraform-providers/terraform-provider-aws/issues/529
  private def createTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(tableName)
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
  }
}
