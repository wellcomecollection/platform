package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.ParallelScanner
import uk.ac.wellcome.platform.reindex.reindex_worker.exceptions.ReindexerException
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
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

      recordsToReindex = reindexJob.maxRecordsPerSegment match {
        case None                       => results.map(extractRecord)
        case Some(maxRecordsPerSegment) =>
          // maxRecordsPerSegment is used to test reindexing or reporting prior to running all records.
          results.take(maxRecordsPerSegment).map(extractRecord)
      }
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
