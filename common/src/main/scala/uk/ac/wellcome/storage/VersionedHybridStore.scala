package uk.ac.wellcome.storage

import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models._
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.type_classes.{IdGetter, VersionGetter, VersionUpdater}
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.annotation.Annotation
import scala.concurrent.Future


case class HybridRecord(
  id: String,
  version: Int,
  s3key: String
) extends Versioned with Id

case class CopyToDynamo() extends Annotation

class VersionedHybridStore[T <: Id] @Inject()(
  sourcedObjectStore: S3ObjectStore[T],
  versionedDao: VersionedDao
) {

  implicit val hybridRecordVersionUpdater = new VersionUpdater[HybridRecord] {
    override def updateVersion(testVersioned: HybridRecord,
                               newVersion: Int): HybridRecord = {
      testVersioned.copy(version = newVersion)
    }
  }

  private case class VersionedHybridObject(
    hybridRecord: HybridRecord,
    s3Object: T
  )

  def updateRecord[O](id: String)(ifNotExisting: => T)(
    ifExisting: T => T)(
    implicit decoder: Decoder[T],
    encoder: Encoder[T],
    enricher: HybridRecordEnricher.Aux[T,O],
    dynamoFormat: DynamoFormat[O],
    versionUpdater: VersionUpdater[O],
    idGetter: IdGetter[O],
    versionGetter: VersionGetter[O]
  ): Future[Unit] = {

    getObject(id).flatMap {
      case Some(VersionedHybridObject(hybridRecord, s3Record)) =>
        val transformedS3Record = ifExisting(s3Record)

        if (transformedS3Record != s3Record) {
          putObject(
            id,
            transformedS3Record,
            enricher.enrichedHybridRecordHList(transformedS3Record, hybridRecord.version))
        } else {
          Future.successful(())
        }

      case None =>
        putObject(
          id = id,
          ifNotExisting,
          enricher.enrichedHybridRecordHList(ifNotExisting, 0))

    }
  }

  def getRecord(id: String)(implicit decoder: Decoder[T]): Future[Option[T]] =
    getObject(id).map { maybeObject =>
      maybeObject.map(_.s3Object)
    }

  private def putObject[O](id: String,
                sourcedObject: T,
                f: (String) => O)(
                 implicit encoder: Encoder[T],
                 dynamoFormat: DynamoFormat[O],
                 versionUpdater: VersionUpdater[O],
                 idGetter: IdGetter[O],
                 versionGetter: VersionGetter[O]
               ) = {
    if (sourcedObject.id != id)
      throw new IllegalArgumentException(
        "ID provided does not match ID in record.")

    val futureKey = sourcedObjectStore.put(sourcedObject)

    futureKey.flatMap { key =>
      versionedDao.updateRecord(f(key))
    }
  }

  private def getObject(id: String)(
    implicit decoder: Decoder[T]): Future[Option[VersionedHybridObject]] = {

    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(hybridRecord) => {
        sourcedObjectStore.get(hybridRecord.s3key).map { s3Record =>
          Some(VersionedHybridObject(hybridRecord, s3Record))
        }
      }
      case None => Future.successful(None)
    }
  }

}
