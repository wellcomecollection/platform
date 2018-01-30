package uk.ac.wellcome.storage

import uk.ac.wellcome.models.Versioned

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext._

case class HybridRecord(
  version: Int,
  sourceId: String,
  sourceName: String,
  s3key: String
) extends Versioned

class VersionedHybridStore {

  def updateRecord[T <: Versioned](record: T): Future[Unit] = ???
    // store the record in S3
    // store the pointer in DynamoDB

}
