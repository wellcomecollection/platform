package uk.ac.wellcome.storage.vhs

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.s3.AmazonS3
import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.models._
import uk.ac.wellcome.storage.dynamo.{UpdateExpressionGenerator, VersionedDao}
import uk.ac.wellcome.storage.s3.{
  KeyPrefixGenerator,
  S3ObjectLocation,
  S3StringStore,
  S3TypedObjectStore
}
import uk.ac.wellcome.storage.type_classes.{
  HybridRecordEnricher,
  IdGetter,
  VersionGetter,
  VersionUpdater
}
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.concurrent.Future

class VersionedHybridStore[T <: Id] @Inject()(
  vhsConfig: VHSConfig,
  s3Client: AmazonS3,
  keyPrefixGenerator: KeyPrefixGenerator[T],
  dynamoDbClient: AmazonDynamoDB
)(implicit encoder: Encoder[T], decoder: Decoder[T]) {

  private val s3StringStore = new S3StringStore(
    s3Client = s3Client,
    s3Config = vhsConfig.s3Config
  )

  val sourcedObjectStore = new S3TypedObjectStore[T](
    stringStore = s3StringStore
  )

  val versionedDao = new VersionedDao(
    dynamoDbClient = dynamoDbClient,
    dynamoConfig = vhsConfig.dynamoConfig
  )

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
    implicit enricher: HybridRecordEnricher.Aux[M, O],
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

  def getRecord(id: String): Future[Option[T]] =
    getObject(id).map { maybeObject =>
      maybeObject.map(_.s3Object)
    }

  private def putObject[O](id: String, sourcedObject: T, f: (String) => O)(
    implicit dynamoFormat: DynamoFormat[O],
    versionUpdater: VersionUpdater[O],
    idGetter: IdGetter[O],
    versionGetter: VersionGetter[O],
    updateExpressionGenerator: UpdateExpressionGenerator[O]
  ) = {
    if (sourcedObject.id != id)
      throw new IllegalArgumentException(
        "ID provided does not match ID in record.")

    val futureUri = sourcedObjectStore.put(
      sourcedObject,
      keyPrefixGenerator.generate(sourcedObject))

    futureUri.flatMap {
      case S3ObjectLocation(_, key) => versionedDao.updateRecord(f(key))
    }
  }

  private def getObject(id: String): Future[Option[VersionedHybridObject]] = {

    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(hybridRecord) => {
        sourcedObjectStore
          .get(
            S3ObjectLocation(
              bucket = vhsConfig.s3Config.bucketName,
              key = hybridRecord.s3key
            )
          )
          .map { s3Record =>
            Some(VersionedHybridObject(hybridRecord, s3Record))
          }
      }
      case None => Future.successful(None)
    }
  }

}
