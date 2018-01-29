package uk.ac.wellcome.locals

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.query.UniqueKey
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Matchers, Suite}
import uk.ac.wellcome.dynamo.SourceData
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.test.utils.DynamoDBLocalClients

import scala.collection.JavaConversions._

trait DynamoDBLocal
    extends BeforeAndAfterEach
    with DynamoDBLocalClients
    with Eventually
    with Matchers { this: Suite =>

  val tableName: String

  deleteTable

  private val table: CreateTableResult = createTable

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearTable
  }

  private def clearTable =
    Scanamo.scan[SourceData](dynamoDbClient)(tableName).map {
      case Right(item) =>
        dynamoDbClient.deleteItem(
          tableName,
          Map("id" -> new AttributeValue(item.id))
        )
      case error =>
        throw new Exception(
          s"Unable to clear the table $tableName error $error")
    }

  private def deleteTable =
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(dynamoDbClient.deleteTable)

  private def createTable =
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

  def dynamoQueryEqualsValue[T: DynamoFormat](key: UniqueKey[_])(
    expectedValue: T) = {

    println(s"Searching DynamoDB for expectedValue = $expectedValue")

    eventually {
      val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)(key).get
      actualValue shouldEqual Right(expectedValue)
    }
  }

}
