package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.Scanamo
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.reindexer.models.ScanamoQueryStream

class MiroReindexTargetService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  @Flag("reindex.target.tableName") targetTableName: String,
  @Flag("reindex.target.reindexShard") targetReindexShard: String = "default",
  metricsSender: MetricsSender)
    extends ReindexTargetService[MiroTransformable](
      dynamoDBClient = dynamoDBClient,
      metricsSender = metricsSender,
      targetTableName = targetTableName,
      targetReindexShard = targetReindexShard
    ) {
  protected val scanamoUpdate: ScanamoUpdate =
    Scanamo.update[MiroTransformable](dynamoDBClient)(targetTableName)

  protected val scanamoQueryStreamFunction: ScanamoQueryStreamFunction =
    ScanamoQueryStream
      .run[MiroTransformable, Boolean]

}
