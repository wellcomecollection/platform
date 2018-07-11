package uk.ac.wellcome.storage.test.fixtures

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.storage.dynamo.DynamoClientFactory
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.collection.JavaConverters._
import scala.util.Random

object LocalDynamoDb {
  case class Table(name: String, index: String)
}

trait LocalDynamoDb extends Eventually with Matchers with ExtendedPatience {

  import LocalDynamoDb._

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port
  private val regionName = "localhost"
  private val accessKey = "access"
  private val secretKey = "secret"

  def dynamoDbLocalEndpointFlags(table: Table): Map[String, String] =
    dynamoClientLocalFlags ++ Map(
      "aws.dynamo.tableName" -> table.name,
      "aws.dynamo.tableIndex" -> table.index
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

  def withSpecifiedLocalDynamoDbTable[R](
    createTable: (AmazonDynamoDB) => Table): Fixture[Table, R] =
    fixture[Table, R](
      create = createTable(dynamoDbClient),
      destroy = { _ =>
        deleteAllTables()
      }
    )

  def withLocalDynamoDbTable[R]: Fixture[Table, R] = fixture[Table, R](
    create = {
      val tableName = Random.alphanumeric.take(10).mkString
      val indexName = Random.alphanumeric.take(10).mkString

      createTable(Table(tableName, indexName))
    },
    destroy = { _ =>
      deleteAllTables()
    }
  )

  def createTable(table: LocalDynamoDb.Table): Table

  private def deleteAllTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .asScala
      .foreach(dynamoDbClient.deleteTable)
  }

  def givenDynamoDbHasItem[T: DynamoFormat](item: T, table: Table) = {
    Scanamo.put(dynamoDbClient)(table.name)(item)
  }

  def assertDynamoDbHasNoItems[T: DynamoFormat](table: Table) = {
    val records = Scanamo.scan[T](dynamoDbClient)(table.name)
    records.size shouldBe 0
  }

  def assertDynamoDbHasItem[T: DynamoFormat](id: String, item: T, table: Table) = {
    val actualRecord = Scanamo.get[T](dynamoDbClient)(
      table.name)('id -> id)
    actualRecord shouldBe Some(Right(item))
  }

  def assertDynamoDbOnlyHasItem[T: DynamoFormat](item: T, table: Table) = {
    val records = Scanamo.scan[T](dynamoDbClient)(table.name)
    records.size shouldBe 1
    records.head shouldBe Right(item)
  }
}
