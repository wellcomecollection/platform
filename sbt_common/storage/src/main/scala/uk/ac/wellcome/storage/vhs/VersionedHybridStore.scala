package uk.ac.wellcome.storage.vhs

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.storage.dynamo.{UpdateExpressionGenerator, VersionedDao}
import uk.ac.wellcome.storage.s3.{
  S3ObjectLocation,
  S3ObjectStore
}
import uk.ac.wellcome.storage.type_classes.{
  HybridRecordEnricher,
  IdGetter,
  VersionGetter,
  VersionUpdater
}
import uk.ac.wellcome.storage.GlobalExecutionContext.context

import scala.concurrent.Future

class VersionedHybridStore[T, S <: S3ObjectStore[T]] @Inject()(
  vhsConfig: VHSConfig,
  s3ObjectStore: S,
  dynamoDbClient: AmazonDynamoDB
) {

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

  private def putObject[O](id: String, t: T, f: (String) => O)(
    implicit dynamoFormat: DynamoFormat[O],
    versionUpdater: VersionUpdater[O],
    idGetter: IdGetter[O],
    versionGetter: VersionGetter[O],
    updateExpressionGenerator: UpdateExpressionGenerator[O]
  ) = {

    val futureUri = s3ObjectStore.put(vhsConfig.s3Config.bucketName)(
      t,
      keyPrefix = buildKeyPrefix(id)
    )

    futureUri.flatMap {
      case S3ObjectLocation(_, key) => versionedDao.updateRecord(f(key))
    }
  }

  // To spread objects evenly in our S3 bucket, we take the last two
  // characters of the ID and reverse them.  This ensures that:
  //
  //  1.  We can find the S3 data corresponding to a given source ID
  //      without consulting the index.
  //
  //  2.  We can identify which record an S3 object is associated with.
  //
  //  3.  Adjacent objects are stored in shards that are far apart,
  //      e.g. 0001 and 0002 are separated by nine shards.
  //
  private def buildKeyPrefix(id: String): String =
    s"${vhsConfig.globalS3Prefix.stripSuffix("/")}/${id.reverse.slice(0, 2)}/$id"

  private def getObject(id: String): Future[Option[VersionedHybridObject]] = {

    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(hybridRecord) => {
        s3ObjectStore
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
