package uk.ac.wellcome.platform.reindex_worker.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, _}
import com.twitter.inject.Logging
import javax.inject.Inject
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.reindex_worker.GlobalExecutionContext.context
import uk.ac.wellcome.platform.reindex_worker.models.{
  ReindexJob,
  ReindexRecord
}
import uk.ac.wellcome.storage.dynamo.{DynamoConfig, VersionedDao}

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDbClient: AmazonDynamoDB,
                               dynamoConfig: DynamoConfig,
                               metricsSender: MetricsSender)
    extends Logging {

  val versionedDao = new VersionedDao(
    dynamoDbClient = dynamoDbClient,
    dynamoConfig = dynamoConfig
  )

  def runReindex(reindexJob: ReindexJob): Future[List[Unit]] =
    Future {
      info(s"ReindexService running $reindexJob")

      val table = Table[ReindexRecord](dynamoConfig.table)

      val index = table.index(indexName = dynamoConfig.index)

      // We start by querying DynamoDB for every record in the reindex shard
      // that has an out-of-date reindexVersion.  If a shard was especially
      // large, this might cause out-of-memory errors -- in practice, we're
      // hoping that the shards/individual records are small enough for this
      // not to be a problem.
      val results: List[Either[DynamoReadError, ReindexRecord]] =
        Scanamo.exec(dynamoDbClient)(
          index.query(
            'reindexShard -> reindexJob.shardId and
              KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
          )
        )

      val outdatedRecords: List[ReindexRecord] = results.map {
        case Left(err: DynamoReadError) => {
          warn(s"Failed to read Dynamo records: $err")
          throw GracefulFailureException(
            new RuntimeException(s"Error in the DynamoDB query: $err")
          )
        }
        case Right(r: ReindexRecord) => r
      }

      // Then we PUT all the records.  It might be more efficient to do a
      // bulk update, but this will do for now.
      outdatedRecords
        .map { record: ReindexRecord =>
          val updatedRecord =
            record.copy(reindexVersion = reindexJob.desiredVersion)
          versionedDao.updateRecord[ReindexRecord](updatedRecord)
        }
        .map { _ => () }
    }
}
