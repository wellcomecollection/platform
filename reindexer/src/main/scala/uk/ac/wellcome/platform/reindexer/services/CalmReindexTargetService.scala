package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.CalmTransformable

class CalmReindexTargetService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  @Flag("reindex.target.tableName") reindexTargetTableName: String,
  metricsSender: MetricsSender)
    extends ReindexTargetService[CalmTransformable](dynamoDBClient,
                                                    metricsSender) {

  protected override val transformableTable: Table[CalmTransformable] =
    Table[CalmTransformable](reindexTargetTableName)

  protected override val scanamoQuery: ScanamoQuery =
    Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
}
