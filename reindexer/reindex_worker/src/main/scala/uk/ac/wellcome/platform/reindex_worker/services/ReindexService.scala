package uk.ac.wellcome.platform.reindex_worker.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, _}
import com.twitter.inject.Logging

import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.reindex_worker.GlobalExecutionContext.context
import uk.ac.wellcome.platform.reindex_worker.models.{ReindexJob, ReindexRecord}
import uk.ac.wellcome.storage.dynamo.{DynamoConfig, VersionedDao}

import scala.concurrent.Future
import scala.util.Try

class ReindexService @Inject()(dynamoDbClient: AmazonDynamoDB,
                               dynamoConfig: DynamoConfig)
    extends Logging {

  val versionedDao = new VersionedDao(
    dynamoDbClient = dynamoDbClient,
    dynamoConfig = dynamoConfig
  )

  def runReindex(reindexJob: ReindexJob): Future[List[Unit]] = {
    info(s"ReindexService running $reindexJob")
    val table = Table[ReindexRecord](dynamoConfig.table)

    val outdatedRecordsFuture: Future[List[ReindexRecord]] = for {
      index: SecondaryIndex[ReindexRecord] <- Future.fromTry(Try {
        table.index(indexName = dynamoConfig.index)
      })

      // We start by querying DynamoDB for every record in the reindex shard
      // that has an out-of-date reindexVersion.  If a shard was especially
      // large, this might cause out-of-memory errors -- in practice, we're
      // hoping that the shards/individual records are small enough for this
      // not to be a problem.
      results: List[Either[DynamoReadError, ReindexRecord]] <- Future {
        Scanamo.exec(dynamoDbClient)(
          index.query(
            'reindexShard -> reindexJob.shardId and
              KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
          )
        )
      }

      outdatedRecords: List[ReindexRecord] = results.map(extractRecord)
    } yield outdatedRecords

    // Then we PUT all the records.  It might be more efficient to do a
    // bulk update, but this will do for now.
    outdatedRecordsFuture.flatMap { outdatedRecords: List[ReindexRecord] =>
      Future.sequence {
        outdatedRecords.map {
          updateIndividualRecord(_, desiredVersion = reindexJob.desiredVersion)
        }
      }
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
