package uk.ac.wellcome.storage

import com.google.inject.Inject
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models.{
  VersionUpdater,
  Versioned,
  VersionedDynamoFormatWrapper
}
import uk.ac.wellcome.s3.VersionedObjectStore

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext._

case class HybridRecord(
  version: Int,
  sourceId: String,
  sourceName: String,
  s3key: String
) extends Versioned

class VersionedHybridStore @Inject()(
  versionedObjectStore: VersionedObjectStore,
  versionedDao: VersionedDao
) {

  implicit val hybridRecordVersionUpdater = new VersionUpdater[HybridRecord] {
    override def updateVersion(testVersioned: HybridRecord,
                               newVersion: Int): HybridRecord = {
      testVersioned.copy(version = newVersion)
    }
  }

  def updateRecord[T <: Versioned](record: T)(
    implicit evidence: VersionedDynamoFormatWrapper[T],
    versionUpdater: VersionUpdater[T],
    encoder: Encoder[T]): Future[Unit] = {
    val futureKey = versionedObjectStore.put(record)

    futureKey.flatMap { key =>
      val hybridRecord = HybridRecord(
        version = record.version,
        sourceId = record.sourceId,
        sourceName = record.sourceName,
        s3key = key
      )

      versionedDao.updateRecord(hybridRecord)
    }
  }

  def getRecord[T <: Versioned](id: String)(
    implicit decoder: Decoder[T]): Future[Option[T]] = {
    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(r) => {
        versionedObjectStore.get[T](r.s3key).map {
          Some(_)
        }
      }
      case None => Future.successful(None)
    }
  }
}
