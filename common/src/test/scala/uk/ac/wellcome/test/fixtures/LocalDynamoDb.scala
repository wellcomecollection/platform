package uk.ac.wellcome.test.fixtures

import scala.util.{Random, Try}
import uk.ac.wellcome.models.{Id, Versioned}
import uk.ac.wellcome.test.utils.DynamoDBLocalClients
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey

import scala.collection.JavaConversions._

trait LocalDynamoDb[T <: Versioned with Id] extends DynamoDBLocalClients {

  implicit val evidence: DynamoFormat[T]

  case class FixtureParams(tableName: String, indexName: String)

  def withLocalDynamoDbTable[R](testWith: TestWith[String, R]): R = {
    val tableName = Random.alphanumeric.take(10).mkString
    val indexName = Random.alphanumeric.take(10).mkString

    try {
      createTable(tableName, indexName)
      testWith(tableName)
    } finally {
      deleteAllTables()
    }
  }

  def withLocalDynamoDbTableAndIndex[R](
    testWith: TestWith[FixtureParams, R]): R = {
    val tableName = Random.alphanumeric.take(10).mkString
    val indexName = Random.alphanumeric.take(10).mkString

    try {
      createTable(tableName, indexName)
      testWith(FixtureParams(tableName, indexName))
    } finally {
      deleteAllTables()
    }
  }

  private def createTable(tableName: String, indexName: String) = {
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
  }

  private def deleteAllTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(dynamoDbClient.deleteTable)
  }

}
