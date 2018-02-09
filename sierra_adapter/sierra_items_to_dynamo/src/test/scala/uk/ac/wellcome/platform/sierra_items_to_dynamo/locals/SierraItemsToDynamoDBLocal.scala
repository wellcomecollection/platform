package uk.ac.wellcome.platform.sierra_items_to_dynamo.locals

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.DynamoDBLocalClients

import scala.collection.JavaConversions._

trait SierraItemsToDynamoDBLocal
    extends BeforeAndAfterEach
    with DynamoDBLocalClients { this: Suite =>

  val tableName = "SierraData"

  val dynamoConfig = DynamoConfig(tableName)

  override def beforeEach(): Unit = {
    super.beforeEach()
    deleteTables()
    createTable()
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
