package uk.ac.wellcome.platform.reindexer.locals

import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.Reindex
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable
}
import uk.ac.wellcome.test.utils.DynamoDBLocalClients

import scala.collection.JavaConversions._

trait DynamoDBLocal extends BeforeAndAfterEach with DynamoDBLocalClients {
  this: Suite =>

  val miroDataTableName = "MiroData"
  val calmDataTableName = "CalmData"
  val reindexTableName = "ReindexTracker"
  val reindexShard = "default"

  deleteTables()
  createReindexTable()
  private val miroDataTable: CreateTableResult = createMiroDataTable()
  private val calmDataTable: CreateTableResult = createCalmDataTable()

  val miroDataStreamArn: String =
    miroDataTable.getTableDescription.getLatestStreamArn
  val calmDataStreamArn: String =
    calmDataTable.getTableDescription.getLatestStreamArn

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearMiroTable()
    clearCalmTable()
    clearReindexTable()
  }

  def generateMiroTransformablesInBatches(
    numberOfBatches: Int,
    reindexVersion: Int = 1): List[MiroTransformable] = {
    import uk.ac.wellcome.test.utils.DynamoConstants._

    val bigString = "_" * maxDynamoItemSizeinKb
    val numberofRequiredPuts = (numberOfBatches * maxDynamoQueryResultSizeInKb) / maxDynamoItemSizeinKb

    (1 to numberofRequiredPuts)
      .map(
        i =>
          MiroTransformable(
            sourceId = s"Image$i",
            MiroCollection = "Collection",
            data = bigString,
            ReindexVersion = reindexVersion
        ))
      .toList
  }

  private def clearReindexTable(): List[DeleteItemResult] =
    Scanamo.scan[Reindex](dynamoDbClient)(reindexTableName).map {
      case Right(reindex) =>
        dynamoDbClient.deleteItem(
          reindexTableName,
          Map(
            "TableName" -> new AttributeValue(reindex.TableName),
            "ReindexShard" -> new AttributeValue(reindex.ReindexShard)
          ))
      case a =>
        throw new Exception(
          s"Unable to clear the table $reindexTableName error $a")
    }

  private def clearMiroTable(): List[DeleteItemResult] =
    Scanamo.scan[MiroTransformable](dynamoDbClient)(miroDataTableName).map {
      case Right(miroTransformable) =>
        dynamoDbClient.deleteItem(
          miroDataTableName,
          Map("MiroID" -> new AttributeValue(miroTransformable.sourceId),
              "MiroCollection" -> new AttributeValue(
                miroTransformable.MiroCollection)))
      case a =>
        throw new Exception(
          s"Unable to clear the table $miroDataTableName error $a")
    }

  private def clearCalmTable(): List[DeleteItemResult] =
    Scanamo.scan[CalmTransformable](dynamoDbClient)(calmDataTableName).map {
      case Right(calmTransformable) =>
        dynamoDbClient.deleteItem(
          calmDataTableName,
          Map(
            "RecordID" -> new AttributeValue(calmTransformable.sourceId),
            "RecordType" -> new AttributeValue(calmTransformable.RecordType)
          ))
      case a =>
        throw new Exception(
          s"Unable to clear the table $calmDataTableName error $a")
    }

  private def deleteTables() = {
    dynamoDbClient
      .listTables()
      .getTableNames
      .foreach(tableName => dynamoDbClient.deleteTable(tableName))
  }

  private def reindexShardDef =
    new AttributeDefinition()
      .withAttributeName("ReindexShard")
      .withAttributeType("S")

  private def reindexVersionDef =
    new AttributeDefinition()
      .withAttributeName("ReindexVersion")
      .withAttributeType("N")

  private def reindexGSI() =
    new GlobalSecondaryIndex()
      .withIndexName("ReindexTracker")
      .withProvisionedThroughput(
        new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
      .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
      .withKeySchema(new KeySchemaElement()
        .withAttributeName("ReindexShard")
        .withKeyType(KeyType.HASH))
      .withKeySchema(new KeySchemaElement()
        .withAttributeName("ReindexVersion")
        .withKeyType(KeyType.RANGE))

  //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
  private def createCalmDataTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(calmDataTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("RecordID")
          .withKeyType(KeyType.HASH))
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("RecordType")
          .withKeyType(KeyType.RANGE))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("RecordID")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("RecordType")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("RefNo")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("AltRefNo")
            .withAttributeType("S"),
          reindexShardDef,
          reindexVersionDef
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
        .withStreamSpecification(new StreamSpecification()
          .withStreamEnabled(true)
          .withStreamViewType(StreamViewType.NEW_IMAGE))
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName("RefNo")
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
            .withProjection(
              new Projection().withProjectionType(ProjectionType.ALL))
            .withKeySchema(new KeySchemaElement()
              .withAttributeName("RefNo")
              .withKeyType(KeyType.HASH)),
          new GlobalSecondaryIndex()
            .withIndexName("AltRefNo")
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
            .withProjection(
              new Projection().withProjectionType(ProjectionType.ALL))
            .withKeySchema(new KeySchemaElement()
              .withAttributeName("AltRefNo")
              .withKeyType(KeyType.HASH)),
          reindexGSI()
        )
    )
  }

  //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
  private def createMiroDataTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(miroDataTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("MiroID")
          .withKeyType(KeyType.HASH))
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("MiroCollection")
          .withKeyType(KeyType.RANGE))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("MiroID")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("MiroCollection")
            .withAttributeType("S"),
          reindexShardDef,
          reindexVersionDef
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
        .withStreamSpecification(new StreamSpecification()
          .withStreamEnabled(true)
          .withStreamViewType(StreamViewType.NEW_IMAGE))
        .withGlobalSecondaryIndexes(reindexGSI())
    )
  }

  private def createReindexTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(reindexTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("TableName")
          .withKeyType(KeyType.HASH))
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("ReindexShard")
          .withKeyType(KeyType.RANGE))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("TableName")
            .withAttributeType("S")
        )
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("ReindexShard")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L)))
  }
}
