package uk.ac.wellcome.platform.reindex_worker.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, _}
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.platform.reindex_worker.GlobalExecutionContext.context
import uk.ac.wellcome.platform.reindex_worker.models.{ReindexJob, ReindexRecord}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class ReindexService @Inject()(dynamoDbClient: AmazonDynamoDB,
                               dynamoConfig: DynamoConfig,
                               snsWriter: SNSWriter)
    extends Logging {

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

    // Then we send an SNS notification for all of the records.  Another
    // application will pick these up and do the writes back to DynamoDB.
    outdatedRecordsFuture.flatMap { outdatedRecords: List[ReindexRecord] =>
      Future.sequence {
        outdatedRecords.map {
          sendIndividualNotification(_, desiredVersion = reindexJob.desiredVersion)
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

  private def sendIndividualNotification(record: ReindexRecord,
                                         desiredVersion: Int): Future[Unit] = {
    val updatedRecord = record.copy(reindexVersion = desiredVersion)
    for {
      _ <- snsWriter.writeMessage(
        message = toJson(updatedRecord).get,
        subject = this.getClass.getSimpleName
      )
    } yield ()
  }
}
