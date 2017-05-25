package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.CalmTransformable

class CalmReindexTargetService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  @Flag("reindex.target.tableName") reindexTargetTableName: String)
    extends ReindexTargetService[CalmTransformable](dynamoDBClient) {

  override val transformableTable: Table[CalmTransformable] =
    Table[CalmTransformable](reindexTargetTableName)

  override val scanamoQuery: ScanamoQuery =
    Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
}
