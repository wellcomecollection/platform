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

  private case class VersionedHybridObject[T <: Versioned](
    hybridRecord: HybridRecord,
    s3Object: T
  )

  def updateRecord[T <: Versioned](sourceName: String, sourceId: String)(ifNotExisting: => T)(ifExisting: T => T)(
    implicit evidence: VersionedDynamoFormatWrapper[T],
    versionUpdater: VersionUpdater[T],
    decoder: Decoder[T],
    encoder: Encoder[T]
  ): Future[Unit] = {
    val id = Versioned.id(sourceName, sourceId)

    getObject[T](id).flatMap {
      case Some(VersionedHybridObject(hybridRecord, s3Record)) =>
        val transformedS3Record = ifExisting(s3Record)

        if(transformedS3Record != s3Record) {
          putObject(id, transformedS3Record, key => hybridRecord.copy(s3key = key))
        } else {
          Future.successful(())
        }

      case None => putObject(id, ifNotExisting, key => HybridRecord(
          version = ifNotExisting.version,
          sourceId = ifNotExisting.sourceId,
          sourceName = ifNotExisting.sourceName,
          s3key = key
        ))
    }
  }

  def getRecord[T <: Versioned](id: String)(
    implicit decoder: Decoder[T]): Future[Option[T]] =
    getObject[T](id).map { maybeObject => maybeObject.map(_.s3Object) }

  private def getObject[T <: Versioned](id: String)(implicit decoder: Decoder[T]):
  Future[Option[VersionedHybridObject[T]]] = {

    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(hybridRecord) => {
        versionedObjectStore.get[T](hybridRecord.s3key).map { s3Record =>
          Some(VersionedHybridObject(hybridRecord, s3Record))
        }
      }
      case None => Future.successful(None)
    }
  }

  private def putObject[T <: Versioned](id: String, versionedObject: T, f: (String) => HybridRecord)(
    implicit encoder: Encoder[T],
    versionUpdater: VersionUpdater[T]
  ) = {
    if(versionedObject.id != id)
      throw new IllegalArgumentException("ID provided does not match ID in record.")

    val futureKey = versionedObjectStore.put(versionedObject)
    futureKey.flatMap { key => versionedDao.updateRecord(f(key)) }
  }
}
