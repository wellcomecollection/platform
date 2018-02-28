package uk.ac.wellcome.storage

import com.google.inject.Inject
import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, Encoder}
import shapeless.ops.hlist.{Collect, LeftFolder, Prepend, Zip}
import shapeless.{Id => ShapelessId, _}
import uk.ac.wellcome.dynamo.{IdGetter, VersionGetter, VersionedDao}
import uk.ac.wellcome.models._
import uk.ac.wellcome.s3.S3ObjectStore
import uk.ac.wellcome.utils.GlobalExecutionContext._

import scala.annotation.Annotation
import scala.concurrent.Future

trait HybridRecordEnricher[T] {
  type Out
  def enrich(record: T, version: Int)(s3key: String): Out
}

object CollectorPoly extends Poly1{
  implicit def some[FT] =
    at[(FT, Some[CopyToDynamo])]{case ((fieldtype, _ )) => fieldtype}
}

object HybridRecordEnricher {
  type Aux[A, R] = HybridRecordEnricher[A] { type Out = R }
  def apply[T](implicit enricher: HybridRecordEnricher[T]) = enricher

  def create[T, O](f: (T, Int,String) => O) = new HybridRecordEnricher[T] {
    type Out = O
    override def enrich(record: T, version: Int)(s3key: String): Out = f(record,version,s3key)
  }

  implicit def enricher[T <: Id, R <: HList,A <: HList, D <: HList, E <: HList, F <: HList](implicit tgen: LabelledGeneric.Aux[T, R], hybridGen: LabelledGeneric.Aux[HybridRecord,F],
                                                                                   annotations: Annotations.Aux[CopyToDynamo, T, A], zipper: Zip.Aux[R :: A :: HNil, D],
                                                                                            collector: Collect.Aux[D, CollectorPoly.type, E], prepend: Prepend[F,E]) = create {
    (record: T, version: Int, s3key: String) => {
      val hybridRecord = HybridRecord(record.id, version, s3key)
      val recordAsHlist = tgen.to(record)
      val recordWithAnnotations = recordAsHlist.zip(annotations.apply())
      val taggedFields = recordWithAnnotations.collect(CollectorPoly)
      hybridGen.to(hybridRecord) ::: taggedFields
    }
  }
}

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

    def putObject(id: String,
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

    getObject(id).flatMap {
      case Some(VersionedHybridObject(hybridRecord, s3Record)) =>
        val transformedS3Record = ifExisting(s3Record)

        if (transformedS3Record != s3Record) {
          putObject(
            id,
            transformedS3Record,
            enricher.enrich(transformedS3Record, hybridRecord.version))
        } else {
          Future.successful(())
        }

      case None =>
        putObject(
          id = id,
          ifNotExisting,
          enricher.enrich(ifNotExisting, 0))

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

}
