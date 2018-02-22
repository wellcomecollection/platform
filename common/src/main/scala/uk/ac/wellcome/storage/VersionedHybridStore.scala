package uk.ac.wellcome.storage

import com.google.inject.Inject
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.dynamo.{Id, IdDynamoFormatWrapper, VersionedDao}
import uk.ac.wellcome.models.transformable.Reindexable
import uk.ac.wellcome.models.{VersionUpdater, Versioned}
import uk.ac.wellcome.s3.ObjectStore

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext._

trait HybridRecord extends Versioned with Id {
  val s3key: String
}

class VersionedHybridStore @Inject()(
  sourcedObjectStore: ObjectStore,
  versionedDao: VersionedDao
) {

//  implicit val hybridRecordVersionUpdater = new VersionUpdater[HybridRecord] {
//    override def updateVersion(testVersioned: HybridRecord,
//                               newVersion: Int): HybridRecord = {
//      testVersioned.copy(version = newVersion)
//    }
//  }

  private case class VersionedHybridObject[T](
    hybridRecord: HybridRecord,
    s3Object: T
  )

  def updateRecord[T <: Id, G <: HybridRecord](
    id: String,
    hybridRecordGenerator: (T, String) => G,
    keyGenerator: T => String)(ifNotExisting: => T)(ifExisting: T => T)(
    implicit evidence: IdDynamoFormatWrapper[G],
    versionUpdater: VersionUpdater[G],
    decoder: Decoder[T],
    encoder: Encoder[T]
  ): Future[Unit] = {

    getObject[T](id).flatMap {
      case Some(VersionedHybridObject(hybridRecord, s3Record)) =>
        val transformedS3Record = ifExisting(s3Record)

        if (transformedS3Record != s3Record) {
          putObject(id,
                    transformedS3Record,
                    hybridRecordGenerator(transformedS3Record, _),
                    keyGenerator)
        } else {
          Future.successful(())
        }

      case None =>
        putObject(
          id = id,
          sourcedObject = ifNotExisting,
          f = hybridRecordGenerator(ifNotExisting, _),
          keyGenerator
        )
    }
  }

  def getRecord[T <: Id](id: String)(
    implicit decoder: Decoder[T]): Future[Option[T]] =
    getObject[T](id).map { maybeObject =>
      maybeObject.map(_.s3Object)
    }

  private def getObject[T <: Id](id: String)(
    implicit decoder: Decoder[T]): Future[Option[VersionedHybridObject[T]]] = {

    val dynamoRecord: Future[Option[HybridRecord]] =
      versionedDao.getRecord[HybridRecord](id = id)

    dynamoRecord.flatMap {
      case Some(hybridRecord) => {
        sourcedObjectStore.get[T](hybridRecord.s3key).map { s3Record =>
          Some(VersionedHybridObject(hybridRecord, s3Record))
        }
      }
      case None => Future.successful(None)
    }
  }

  private def putObject[T <: Id](id: String,
                                 sourcedObject: T,
                                 f: (String) => HybridRecord,
                                 keyGenerator: T => String)(
    implicit encoder: Encoder[T]
  ) = {
    if (sourcedObject.id != id)
      throw new IllegalArgumentException(
        "ID provided does not match ID in record.")

    val futureKey = sourcedObjectStore.put(sourcedObject)(keyGenerator)

    futureKey.flatMap { key =>
      versionedDao.updateRecord(f(key))
    }
  }
}
