package uk.ac.wellcome.test.utils

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.{
  AmazonDynamoDB,
  AmazonDynamoDBClientBuilder,
  AmazonDynamoDBStreams,
  AmazonDynamoDBStreamsClientBuilder
}
import com.gu.scanamo.Scanamo
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.ac.wellcome.models.{
  CalmTransformable,
  Identifier,
  MiroTransformable,
  Reindex
}

import scala.collection.JavaConversions._

trait DynamoDBLocal extends BeforeAndAfterEach { this: Suite =>

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port

  private val accessKey = "access"
  private val secretKey = "secret"
  val dynamoDbLocalEndpointFlags: Map[String, String] =
    Map(
      "aws.dynamoDb.endpoint" -> dynamoDBEndPoint,
      "aws.dynamoDb.streams.endpoint" -> dynamoDBEndPoint,
      "aws.region" -> "localhost",
      "aws.accessKey" -> accessKey,
      "aws.secretKey" -> secretKey
    )

  private val dynamoDBLocalCredentialsProvider =
    new AWSStaticCredentialsProvider(
      new BasicAWSCredentials(accessKey, secretKey))

  val dynamoDbClient: AmazonDynamoDB = AmazonDynamoDBClientBuilder
    .standard()
    .withCredentials(dynamoDBLocalCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()

  val identifiersTableName = "Identifiers"
  val miroDataTableName = "MiroData"
  val calmDataTableName = "CalmData"
  val reindexTableName = "ReindexTracker"

  deleteTables()
  createIdentifiersTable()
  createReindexTable()
  private val miroDataTable: CreateTableResult = createMiroDataTable()
  private val calmDataTable: CreateTableResult = createCalmDataTable()

  val miroDataStreamArn: String =
    miroDataTable.getTableDescription.getLatestStreamArn
  val calmDataStreamArn: String =
    calmDataTable.getTableDescription.getLatestStreamArn

  val streamsClient: AmazonDynamoDBStreams = AmazonDynamoDBStreamsClientBuilder
    .standard()
    .withCredentials(dynamoDBLocalCredentialsProvider)
    .withEndpointConfiguration(
      new EndpointConfiguration(dynamoDBEndPoint, "localhost"))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    clearIdentifiersTable()
    clearMiroTable()
    clearCalmTable()
    clearReindexTable()
  }

  private def clearReindexTable(): List[DeleteItemResult] =
    Scanamo.scan[Reindex](dynamoDbClient)(reindexTableName).map {
      case Right(reindex) =>
        dynamoDbClient.deleteItem(
          reindexTableName,
          Map("TableName" -> new AttributeValue(reindex.TableName)))
      case a =>
        throw new Exception(
          s"Unable to clear the table $reindexTableName error $a")
    }

  private def clearIdentifiersTable(): List[DeleteItemResult] =
    Scanamo.scan[Identifier](dynamoDbClient)(identifiersTableName).map {
      case Right(identifier) =>
        dynamoDbClient.deleteItem(
          identifiersTableName,
          Map("CanonicalID" -> new AttributeValue(identifier.CanonicalID)))
      case a =>
        throw new Exception(
          s"Unable to clear the table $identifiersTableName error $a")
    }

  private def clearMiroTable(): List[DeleteItemResult] =
    Scanamo.scan[MiroTransformable](dynamoDbClient)(miroDataTableName).map {
      case Right(miroTransformable) =>
        dynamoDbClient.deleteItem(
          miroDataTableName,
          Map("MiroID" -> new AttributeValue(miroTransformable.MiroID),
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
            "RecordID" -> new AttributeValue(calmTransformable.RecordID),
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

  //TODO delete and use terraform apply once this issue is fixed: https://github.com/hashicorp/terraform/issues/11926
  private def createIdentifiersTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(identifiersTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("CanonicalID")
          .withKeyType(KeyType.HASH))
        .withGlobalSecondaryIndexes(
          new GlobalSecondaryIndex()
            .withIndexName("MiroID")
            .withProvisionedThroughput(new ProvisionedThroughput()
              .withReadCapacityUnits(1L)
              .withWriteCapacityUnits(1L))
            .withProjection(new Projection()
              .withProjectionType(ProjectionType.ALL))
            .withKeySchema(new KeySchemaElement()
              .withAttributeName("MiroID")
              .withKeyType(KeyType.HASH)))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("CanonicalID")
            .withAttributeType("S"),
          new AttributeDefinition()
            .withAttributeName("MiroID")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L)))
  }

  private def createReindexTable() = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(reindexTableName)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("TableName")
          .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("TableName")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L)))
  }
}
