package uk.ac.wellcome.storage.test.fixtures

import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import uk.ac.wellcome.storage.dynamo.{DynamoConfig, VersionedDao}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

trait LocalDynamoDbVersioned extends LocalDynamoDb {

  override def createTable(table: Table): Table = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
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
            .withIndexName(table.index)
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
      waitUntilActive(dynamoDbClient, table.name)
    }
    table
  }

  def withVersionedDao[R](table: Table)(
    testWith: TestWith[VersionedDao, R]): R = {
    val dynamoConfig = DynamoConfig(table = table.name, index = table.index)
    val dao = new VersionedDao(dynamoDbClient, dynamoConfig)
    testWith(dao)
  }
}
