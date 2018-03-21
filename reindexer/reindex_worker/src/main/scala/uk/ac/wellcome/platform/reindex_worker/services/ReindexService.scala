package uk.ac.wellcome.platform.reindex_worker.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, _}
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.reindex_worker.models.{
  ReindexJob,
  ReindexRecord
}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               metricsSender: MetricsSender,
                               versionedDao: VersionedDao,
                               dynamoConfig: DynamoConfig,
                               @Flag("aws.dynamo.indexName") indexName: String)
    extends Logging {

  def runReindex(reindexJob: ReindexJob): Future[List[Unit]] = {
    info(s"ReindexService running $reindexJob")

    val table = Table[ReindexRecord](dynamoConfig.table)

    val index = table.index(indexName)

    // We start by querying DynamoDB for every record in the reindex shard
    // that has an out-of-date reindexVersion.  If a shard was especially
    // large, this might cause out-of-memory errors -- in practice, we're
    // hoping that the shards/individual records are small enough for this
    // not to be a problem.
    val futureResults: Future[List[Either[DynamoReadError, ReindexRecord]]] =
      Future {
        Scanamo.exec(dynamoDBClient)(
          index.query(
            'reindexShard -> reindexJob.shardId and
              KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
          )
        )
      }

    val futureOutdatedRecords: Future[List[ReindexRecord]] =
      futureResults.map { results =>
        results.map { extractRecord(_) }
      }

    // Then we PUT all the records.  It might be more efficient to do a
    // bulk update, but this will do for now.
    futureOutdatedRecords.flatMap { (outdatedRecords: List[ReindexRecord]) =>
      Future.sequence(outdatedRecords.map {
        updateIndividualRecord(_, desiredVersion = reindexJob.desiredVersion)
      })
    }
  }

  private def extractRecord(
    scanamoResult: Either[DynamoReadError, ReindexRecord]): ReindexRecord =
    scanamoResult match {
      case Left(err: DynamoReadError) => {
        warn(s"Failed to read Dynamo records: $err")
        throw GracefulFailureException(
          new RuntimeException(s"Error in the DynamoDB query: $err")
        )
      }
      case Right(r: ReindexRecord) => r
    }

  private def updateIndividualRecord(record: ReindexRecord,
                                     desiredVersion: Int): Future[Unit] = {
    val updatedRecord = record.copy(reindexVersion = desiredVersion)

    versionedDao.updateRecord[ReindexRecord](updatedRecord)
  }
}
