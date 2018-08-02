package uk.ac.wellcome.platform.reindex.creator.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.{Scanamo, SecondaryIndex, Table}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query._
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.models.reindexer.ReindexableRecord
import uk.ac.wellcome.platform.reindex.creator.exceptions.ReindexerException
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/** Find IDs for records in the SourceData table that need reindexing.
  *
  * This class should only be doing reading -- deciding how to act on records
  * that need reindexing is the responsibility of another class.
  */
class RecordReader @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def findRecordsForReindexing(reindexJob: ReindexJob): Future[List[String]] = {
    debug(s"Finding records that need reindexing for $reindexJob")

    val table = Table[ReindexableRecord](dynamoConfig.table)

    for {
      index: SecondaryIndex[ReindexableRecord] <- Future.fromTry(Try {
        table.index(indexName = dynamoConfig.index)
      })

      // We start by querying DynamoDB for every record in the reindex shard
      // that has an out-of-date reindexVersion.  If a shard was especially
      // large, this might cause out-of-memory errors -- in practice, we're
      // hoping that the shards/individual records are small enough for this
      // not to be a problem.
      results: List[Either[DynamoReadError, ReindexableRecord]] <- Future {
        Scanamo.exec(dynamoDbClient)(
          index.query(
            'reindexShard -> reindexJob.shardId and
              KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
          )
        )
      }

      recordIdsToReindex: List[String] = results.map(extractRecordID)
    } yield recordIdsToReindex
  }

  private def extractRecordID(
    scanamoResult: Either[DynamoReadError, ReindexableRecord]): String =
    scanamoResult match {
      case Left(err: DynamoReadError) => {
        warn(s"Failed to read Dynamo records: $err")
        throw ReindexerException(s"Error in the DynamoDB query: $err")
      }
      case Right(r: ReindexableRecord) => r.id
    }
}
