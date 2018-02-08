package uk.ac.wellcome.storage

import com.google.inject.Inject
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models.{VersionUpdater, Versioned, VersionedDynamoFormatWrapper}
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

  def updateRecord[T <: Versioned](sourceName: String, sourceId: String)(ifNotExisting: => T)(ifExisting: T => T)(
    implicit evidence: VersionedDynamoFormatWrapper[T],
    versionUpdater: VersionUpdater[T],
    decoder: Decoder[T],
    encoder: Encoder[T]
  ): Future[Unit] = {
    val id = Versioned.id(sourceName, sourceId)
    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    val eventualMaybeTuple = dynamoRecord.flatMap {
      case Some(r) => {
        versionedObjectStore.get[T](r.s3key).map { s3Record =>
          Some((r, s3Record))
        }
      }
      case None => Future.successful(None)
    }

    eventualMaybeTuple.flatMap {
        case Some((hybridRecord, s3Record)) =>
          val transformedS3Record = ifExisting(s3Record)

          if(transformedS3Record.id != id)
            throw new IllegalArgumentException("ID provided does not match ID in record.")

          if(transformedS3Record != s3Record) {

            val futureKey = versionedObjectStore.put(transformedS3Record)
            futureKey.flatMap { key =>
              val newHybridRecord = hybridRecord.copy(s3key = key)

              versionedDao.updateRecord(newHybridRecord)
            }
          }else {
            Future.successful(())
          }
        case None =>

          if(ifNotExisting.id != id)
            throw new IllegalArgumentException("ID provided does not match ID in record.")

          val futureKey = versionedObjectStore.put(ifNotExisting)

          futureKey.flatMap { key =>
            val hybridRecord = HybridRecord(
              version = ifNotExisting.version,
              sourceId = ifNotExisting.sourceId,
              sourceName = ifNotExisting.sourceName,
              s3key = key
            )

            versionedDao.updateRecord(hybridRecord)
          }
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
