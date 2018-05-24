package uk.ac.wellcome.storage.vhs

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectStore}
import uk.ac.wellcome.storage.type_classes._
import uk.ac.wellcome.storage.type_classes.Migration._
import uk.ac.wellcome.storage.GlobalExecutionContext.context

import scala.concurrent.Future

class VersionedHybridStore[T, Store <: S3ObjectStore[T]] @Inject()(
  vhsConfig: VHSConfig,
  s3ObjectStore: Store,
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
  // You pass it a record and optionally a case class containing some metadata.
  // The HybridRecordEnricher combines this with the HybridRecord, and stores
  // both of them as a single row in DynamoDB.
  //
  def updateRecord[DynamoRow, Metadata](id: String)(ifNotExisting: => T)(
    ifExisting: T => T)(metadata: Metadata = EmptyMetadata())(
    implicit enricher: HybridRecordEnricher.Aux[Metadata, DynamoRow],
    dynamoFormat: DynamoFormat[DynamoRow],
    versionUpdater: VersionUpdater[DynamoRow],
    idGetter: IdGetter[DynamoRow],
    versionGetter: VersionGetter[DynamoRow],
    updateExpressionGenerator: UpdateExpressionGenerator[DynamoRow],
    migrationH: Migration[DynamoRow, HybridRecord]
  ): Future[Unit] = {

    getObject[DynamoRow](id).flatMap {
      case Some(VersionedHybridObject(storedHybridRecord, storedS3Record)) =>
        val transformedS3Record = ifExisting(storedS3Record)

        if (transformedS3Record != storedS3Record) {
          putObject(
            id,
            transformedS3Record,
            enricher
              .enrichedHybridRecordHList(
                id = id,
                metadata = metadata,
                version = storedHybridRecord.version)
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
    getObject[HybridRecord](id).map { maybeObject =>
      maybeObject.map(_.s3Object)
    }

  private def putObject[DynamoRow](id: String, t: T, f: (String) => DynamoRow)(
    implicit dynamoFormat: DynamoFormat[DynamoRow],
    versionUpdater: VersionUpdater[DynamoRow],
    idGetter: IdGetter[DynamoRow],
    versionGetter: VersionGetter[DynamoRow],
    updateExpressionGenerator: UpdateExpressionGenerator[DynamoRow]
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

  private def getObject[DynamoRow](
    id: String)(implicit dynamoFormat: DynamoFormat[DynamoRow], migrationH: Migration[DynamoRow, HybridRecord]): Future[Option[VersionedHybridObject]] = {

    val dynamoRecord: Future[Option[DynamoRow]] =
      versionedDao.getRecord[DynamoRow](id = id)

    dynamoRecord.flatMap {
      case Some(dynamoRow) => {
        val hybridRecord = dynamoRow.migrateTo[HybridRecord]
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
