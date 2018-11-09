package uk.ac.wellcome.platform.reindex.reindex_worker.services

import java.util

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.google.inject.Inject
import com.gu.scanamo.{DynamoFormat, ScanamoFree}
import com.gu.scanamo.error.DynamoReadError
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  MaxRecordsScanner,
  ParallelScanner
}
import uk.ac.wellcome.platform.reindex.reindex_worker.exceptions.ReindexerException
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{
  CompleteReindexJob,
  PartialReindexJob,
  ReindexJob
}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{HybridRecord, VHSIndexEntry}

import scala.concurrent.{ExecutionContext, Future}

/** Find IDs for records in the SourceData table that need reindexing.
  *
  * This class should only be doing reading -- deciding how to act on records
  * that need reindexing is the responsibility of another class.
  */
class RecordReader @Inject()(
  maxRecordsScanner: MaxRecordsScanner,
  parallelScanner: ParallelScanner
)(implicit ec: ExecutionContext)
    extends Logging {

  def findRecordsForReindexing[M](reindexJob: ReindexJob)(
    implicit hybridRecordDynamoFormat: DynamoFormat[HybridRecord],
    metadataDynamoFormat: DynamoFormat[M]): Future[List[VHSIndexEntry[M]]] = {
    debug(s"Finding records that need reindexing for $reindexJob")

    for {
      // We start by querying DynamoDB for every record we want to reindex.
      // If we requested reindexing a particularly large shard, this might
      // cause out-of-memory errors -- in practice, we're hoping that the
      // shards/individual records are small enough for this not to be a problem.
      attributeValues: List[util.Map[String, AttributeValue]] <- reindexJob match {
        case CompleteReindexJob(segment, totalSegments) =>
          parallelScanner
            .scan(
              segment = segment,
              totalSegments = totalSegments
            )
        case PartialReindexJob(maxRecords) =>
          maxRecordsScanner.scan(maxRecords = maxRecords)
      }

      recordsToReindex: List[VHSIndexEntry[M]] = attributeValues.map { av =>
        parseResult[M](av)
      }
    } yield recordsToReindex
  }

  private def parseResult[M](attributeValues: util.Map[String, AttributeValue])(
    implicit hybridRecordDynamoFormat: DynamoFormat[HybridRecord],
    metadataDynamoFormat: DynamoFormat[M]): VHSIndexEntry[M] = {
    // Take the Map[String, AttributeValue], and convert it into an
    // instance of the case class `T`.  This is using a Scanamo helper --
    // I worked this out by looking at [[ScanamoFree.get]].
    //
    // https://github.com/scanamo/scanamo/blob/12554b8e24ef8839d5e9dd9a4f42ae130e29b42b/scanamo/src/main/scala/com/gu/scanamo/ScanamoFree.scala#L62
    //
    val maybeHybridRecord: Either[DynamoReadError, HybridRecord] =
      ScanamoFree.read[HybridRecord](attributeValues)
    val maybeMetadata: Either[DynamoReadError, M] =
      ScanamoFree.read[M](attributeValues)

    (maybeHybridRecord, maybeMetadata) match {
      case (Right(hybridRecord: HybridRecord), Right(metadata)) =>
        VHSIndexEntry(hybridRecord = hybridRecord, metadata = metadata)
      case _ =>
        throw ReindexerException(
          s"Error when parsing $attributeValues as VHSIndexEntry")
    }
  }
}
