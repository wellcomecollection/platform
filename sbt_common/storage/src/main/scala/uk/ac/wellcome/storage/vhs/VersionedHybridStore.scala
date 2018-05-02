package uk.ac.wellcome.storage.vhs

import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws.S3Config
import uk.ac.wellcome.storage.dynamo.{UpdateExpressionGenerator, VersionedDao}
import uk.ac.wellcome.storage.type_classes.HybridRecordEnricher
import uk.ac.wellcome.s3.{S3ObjectLocation, S3ObjectStore}
import uk.ac.wellcome.type_classes.{IdGetter, VersionGetter, VersionUpdater}
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.concurrent.Future

case class HybridRecord(
  id: String,
  version: Int,
  s3key: String
) extends Versioned
    with Id

class VersionedHybridStore[T <: Id] @Inject()(
  s3Config: S3Config,
  sourcedObjectStore: S3ObjectStore[T],
  versionedDao: VersionedDao
) {

  case class EmptyMetadata()
  private case class VersionedHybridObject(
    hybridRecord: HybridRecord,
    s3Object: T
  )

  // Store a single record in DynamoDB.
  //
  // You pass it a record and optionally a case class containing some metadata (type M).
  // The metadata parameter is useful if you have some additional detail you don't want
  // to store on your primary model.
  //
  // The two are combined into a single HList with the HybridRecordEnricher of type O,
  // and this record is what gets saved to DynamoDB.
  //
  def updateRecord[O, M](id: String)(ifNotExisting: => T)(ifExisting: T => T)(
    metadata: M = EmptyMetadata())(
    implicit decoder: Decoder[T],
    encoder: Encoder[T],
    enricher: HybridRecordEnricher.Aux[M, O],
    dynamoFormat: DynamoFormat[O],
    versionUpdater: VersionUpdater[O],
    idGetter: IdGetter[O],
    versionGetter: VersionGetter[O],
    updateExpressionGenerator: UpdateExpressionGenerator[O]
  ): Future[Unit] = {

    getObject(id).flatMap {
      case Some(VersionedHybridObject(hybridRecord, s3Record)) =>
        val transformedS3Record = ifExisting(s3Record)

        if (transformedS3Record != s3Record) {
          putObject(
            id,
            transformedS3Record,
            enricher
              .enrichedHybridRecordHList(
                id = id,
                metadata = metadata,
                version = hybridRecord.version)
          )
        } else {
          Future.successful(())
        }

      case None =>
        putObject(
          id = id,
          ifNotExisting,
          enricher.enrichedHybridRecordHList(
            id = id,
            metadata = metadata,
            version = 0
          )
        )

    }
  }

  def getRecord(id: String)(implicit decoder: Decoder[T]): Future[Option[T]] =
    getObject(id).map { maybeObject =>
      maybeObject.map(_.s3Object)
    }

  private def putObject[O](id: String, sourcedObject: T, f: (String) => O)(
    implicit encoder: Encoder[T],
    dynamoFormat: DynamoFormat[O],
    versionUpdater: VersionUpdater[O],
    idGetter: IdGetter[O],
    versionGetter: VersionGetter[O],
    updateExpressionGenerator: UpdateExpressionGenerator[O]
  ) = {
    if (sourcedObject.id != id)
      throw new IllegalArgumentException(
        "ID provided does not match ID in record.")

    val futureUri = sourcedObjectStore.put(sourcedObject)

    futureUri.flatMap {
      case S3ObjectLocation(_, key) => versionedDao.updateRecord(f(key))
    }
  }

  private def getObject(id: String)(
    implicit decoder: Decoder[T]): Future[Option[VersionedHybridObject]] = {

    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(hybridRecord) => {
        sourcedObjectStore
          .get(
            S3ObjectLocation(s3Config.bucketName, hybridRecord.s3key)
          )
          .map { s3Record =>
            Some(VersionedHybridObject(hybridRecord, s3Record))
          }
      }
      case None => Future.successful(None)
    }
  }

}
