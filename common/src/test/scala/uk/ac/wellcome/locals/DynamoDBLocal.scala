package uk.ac.wellcome.locals

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Matchers, Suite}
import uk.ac.wellcome.models.{Id, Sourced, Versioned}
import uk.ac.wellcome.test.utils.{DynamoDBLocalClients, ExtendedPatience}

import scala.collection.JavaConversions._

trait DynamoDBLocal[T <: Versioned with Id]
    extends BeforeAndAfterEach
    with DynamoDBLocalClients
    with Eventually
    with Matchers
    with ExtendedPatience { this: Suite =>

  implicit val evidence: DynamoFormat[T]
  val tableName: String

  val indexName: String = "reindexTracker"
  deleteTable()
  createTable()

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearTable()
  }

  private def clearTable(): List[DeleteItemResult] =
    Scanamo.scan[T](dynamoDbClient)(tableName).map {
      case Right(item) =>
        dynamoDbClient.deleteItem(
          tableName,
          Map("id" -> new AttributeValue(item.id))
        )
      case error =>
        throw new Exception(
          s"Unable to clear the table $tableName error $error")
    }

  private def deleteTable() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(dynamoDbClient.deleteTable)
  }

  private def createTable() = {
    println(s"Creating local DynamoDB table: $tableName")

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

    eventually {
      dynamoDbClient
        .describeTable(tableName)
        .getTable
        .getTableStatus shouldBe "ACTIVE"

    }
  }

  def dynamoQueryEqualsValue(key: UniqueKey[_])(expectedValue: T)(
    implicit evidence: DynamoFormat[T]) = {

    println(s"Searching DynamoDB for expectedValue = $expectedValue")

    eventually {
      val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)(key).get
      actualValue shouldEqual Right(expectedValue)
    }
  }

}
