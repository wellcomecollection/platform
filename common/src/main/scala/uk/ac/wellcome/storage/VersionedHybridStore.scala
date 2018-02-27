package uk.ac.wellcome.storage

import com.google.inject.Inject
import io.circe.{Decoder, Encoder}
import shapeless.ops.hlist.{LeftFolder, Prepend, Zip}
import shapeless.{Id => ShapelessId, _}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.models._
import uk.ac.wellcome.s3.S3ObjectStore
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

  object Collector extends Poly2{
    implicit def some[L <: HList, FT] =
      at[L, (FT, Some[CopyToDynamo])]{case (accumulatorList,(fieldtype, _) ) => fieldtype :: accumulatorList}
    implicit def none[L <: HList, FT] =
      at[L, (FT, None.type)]{case (accumulatorList,_) => accumulatorList }
  }

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

  private def buildHybridRecordHList[R <: HList,A <: HList, D <: HList, E <: HList, F <: HList](record: T, version: Int)(s3key: String)
                                    (implicit tgen: LabelledGeneric.Aux[T, R], hybridGen: LabelledGeneric.Aux[HybridRecord,F],
                                     annotations: Annotations.Aux[CopyToDynamo, T, A], zipper: Zip.Aux[R :: A :: HNil, D],
                                     leftFolder: LeftFolder.Aux[D, HList, Collector.type, E], prepend: Prepend[F,E]) = {
    val hybridRecord = HybridRecord(record.id, version, s3key)

    val repr= tgen.to(record)

    val value = repr.zip(annotations.apply())

    val taggedFields = value.foldLeft(HNil: HList)(Collector)

    hybridGen.to(hybridRecord) ::: taggedFields

  }

  def updateRecord(id: String)(ifNotExisting: => T)(
    ifExisting: T => T)(
    implicit decoder: Decoder[T],
    encoder: Encoder[T]
  ): Future[Unit] = {

    getObject(id).flatMap {
      case Some(VersionedHybridObject(hybridRecord, s3Record)) =>
        val transformedS3Record = ifExisting(s3Record)

        if (transformedS3Record != s3Record) {
          putObject(
            id,
            transformedS3Record,
            buildHybridRecordHList(transformedS3Record, hybridRecord.version))
        } else {
          Future.successful(())
        }

      case None =>
        putObject(
          id = id,
          ifNotExisting,
          f = buildHybridRecordHList(ifNotExisting, 0))

    }
  }

  def getRecord(id: String)(implicit decoder: Decoder[T]): Future[Option[T]] =
    getObject(id).map { maybeObject =>
      maybeObject.map(_.s3Object)
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

  private def putObject[A <: HList](id: String,
                        sourcedObject: T,
                        f: (String) => A)(
    implicit encoder: Encoder[T]
  ) = {
    if (sourcedObject.id != id)
      throw new IllegalArgumentException(
        "ID provided does not match ID in record.")

    val futureKey = sourcedObjectStore.put(sourcedObject)

    futureKey.flatMap { key =>
      versionedDao.updateRecord(f(key))
    }
  }
}
