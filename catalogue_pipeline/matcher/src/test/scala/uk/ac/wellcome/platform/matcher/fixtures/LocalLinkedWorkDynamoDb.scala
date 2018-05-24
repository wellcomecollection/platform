package uk.ac.wellcome.platform.matcher.fixtures

import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table

trait LocalLinkedWorkDynamoDb
  extends LocalDynamoDb
{
  override def createTable(table: Table): Table = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("workId")
          .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("workId")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("setId")
            .withAttributeType("S"),
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
                .withAttributeName("setId")
                .withKeyType(KeyType.HASH)
            )
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))))
    eventually {
      waitUntilActive(dynamoDbClient, table.name)
    }
    table
  }
}
