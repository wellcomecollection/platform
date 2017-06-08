package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.CalmTransformable
import uk.ac.wellcome.utils.ScanamoQueryStream

class CalmReindexTargetService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  @Flag("reindex.target.tableName") reindexTargetTableName: String,
  metricsSender: MetricsSender)
    extends ReindexTargetService[CalmTransformable](dynamoDBClient,
                                                    metricsSender,
                                                    reindexTargetTableName) {

  protected val scanamoUpdate: ScanamoUpdate =
    Scanamo.update[CalmTransformable](dynamoDBClient)(reindexTargetTableName)

  protected val scanamoQueryStreamFunction: ScanamoQueryStreamFunction =
    ScanamoQueryStream
      .run[CalmTransformable, Either[DynamoReadError, CalmTransformable]]
}
