package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.models.CalmTransformable
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindexer.lib.ReindexService

class CalmReindexService @Inject()(
  reindexTrackerService: ReindexTrackerService,
  dynamoDBClient: AmazonDynamoDB,
  dynamoConfigs: Map[String, DynamoConfig],
  @Flag("reindex.target.tableName") reindexTargetTableName: String)
    extends ReindexService[CalmTransformable](reindexTrackerService,
                                              dynamoDBClient,
                                              dynamoConfigs,
                                              reindexTargetTableName,
                                              "calm") {

  override val transformableTable: Table[CalmTransformable] =
    Table[CalmTransformable](reindexTargetTableName)

  override val scanamoQuery: ScanamoQuery =
    Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
}
