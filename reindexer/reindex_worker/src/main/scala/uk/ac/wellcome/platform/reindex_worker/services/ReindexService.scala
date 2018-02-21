package uk.ac.wellcome.platform.reindex_worker.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo._
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.inject.Logging
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{
  Sourced,
  SourcedDynamoFormatWrapper,
  VersionUpdater
}
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               metricsSender: MetricsSender,
                               versionedDao: VersionedDao,
                               dynamoConfig: DynamoConfig)
    extends Logging {

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Sourced
    .toSourcedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  implicit val versionUpdater = new VersionUpdater[HybridRecord] {
    override def updateVersion(record: HybridRecord,
                               newVersion: Int): HybridRecord =
      record.copy(version = newVersion)
  }

  def runReindex(reindexJob: ReindexJob)(
    implicit evidence: SourcedDynamoFormatWrapper[HybridRecord])
    : Future[List[Unit]] = {

    info(s"ReindexService running $reindexJob")

    val table = Table[HybridRecord](dynamoConfig.table)

    // TODO: The name of the GSI should be a config flag.
    val index = table.index("reindexTracker")

    // We start by querying DynamoDB for every record in the reindex shard
    // that has an out-of-date reindexVersion.  If a shard was especially
    // large, this might cause out-of-memory errors -- in practice, we're
    // hoping that the shards/individual records are small enough for this
    // not to be a problem.
    val queryResults: List[Either[DynamoReadError, HybridRecord]] =
      Scanamo.exec(dynamoDBClient)(
        index.query(
          'reindexShard -> reindexJob.shardId and
            KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
        )
      )

    val outdatedRecords = queryResults.map {
      case Left(err: DynamoReadError) => {
        warn(s"Failed to read Dynamo records for $reindexJob: $err")
        throw GracefulFailureException(
          new RuntimeException(s"Error in the DynamoDB query: $err")
        )
      }
      case Right(r: HybridRecord) => r
    }

    // Then we PUT all the records.  It might be more efficient to do a
    // bulk update, but this will do for now.
    val updates: List[Future[Unit]] = outdatedRecords.map { hybridRecord =>
      val updatedRecord =
        hybridRecord.copy(reindexVersion = reindexJob.desiredVersion)
      versionedDao.updateRecord[HybridRecord](updatedRecord)(evidence =
                                                               evidence,
                                                             versionUpdater =
                                                               versionUpdater)
    }

    Future.sequence(updates)
  }
}
