package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.Scanamo
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.miro.MiroTransformable
import uk.ac.wellcome.utils.ScanamoQueryStream

class MiroReindexTargetService @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  @Flag("reindex.target.tableName") reindexTargetTableName: String,
  metricsSender: MetricsSender)
    extends ReindexTargetService[MiroTransformable](dynamoDBClient,
                                                    metricsSender,
                                                    reindexTargetTableName) {

  protected val scanamoUpdate: ScanamoUpdate =
    Scanamo.update[MiroTransformable](dynamoDBClient)(reindexTargetTableName)

  protected val scanamoQueryStreamFunction: ScanamoQueryStreamFunction =
    ScanamoQueryStream
      .run[MiroTransformable, Boolean]

}
