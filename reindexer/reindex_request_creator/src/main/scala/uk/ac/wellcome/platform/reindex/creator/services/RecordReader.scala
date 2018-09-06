package uk.ac.wellcome.platform.reindex.creator.services

import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.reindex.creator.dynamo.ParallelScanner
import uk.ac.wellcome.platform.reindex.creator.exceptions.ReindexerException
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

/** Find IDs for records in the SourceData table that need reindexing.
  *
  * This class should only be doing reading -- deciding how to act on records
  * that need reindexing is the responsibility of another class.
  */
class RecordReader @Inject()(
  parallelScanner: ParallelScanner
)(implicit ec: ExecutionContext)
    extends Logging {

  def findRecordsForReindexing(
    reindexJob: ReindexJob): Future[List[HybridRecord]] = {
    debug(s"Finding records that need reindexing for $reindexJob")

    for {
      // We start by querying DynamoDB for every record in the reindex shard.
      // If a shard was especially large, this might cause out-of-memory errors
      // -- in practice, we're hoping that the shards/individual records are
      // small enough for this not to be a problem.
      results: List[Either[DynamoReadError, HybridRecord]] <- parallelScanner
        .scan[HybridRecord](
          segment = reindexJob.segment,
          totalSegments = reindexJob.totalSegments
        )

      recordsToReindex: List[HybridRecord] = results.map(extractRecord)
    } yield recordsToReindex
  }

  private def extractRecord(
    scanamoResult: Either[DynamoReadError, HybridRecord]): HybridRecord =
    scanamoResult match {
      case Left(err: DynamoReadError) => {
        warn(s"Failed to read Dynamo records: $err")
        throw ReindexerException(s"Error in the DynamoDB query: $err")
      }
      case Right(hr: HybridRecord) => hr
    }
}
