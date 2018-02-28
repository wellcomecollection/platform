package uk.ac.wellcome.storage

import shapeless.ops.hlist.{Collect, Prepend, Zip}
import shapeless.{Id => ShapelessId, _}
import uk.ac.wellcome.models.Id


trait HybridRecordEnricher[T] {
  type Out
  def enrichedHybridRecordHList(record: T, version: Int)(s3key: String): Out
}

object HybridRecordEnricher {
  type Aux[A, R] = HybridRecordEnricher[A] { type Out = R }
  def apply[T](implicit enricher: HybridRecordEnricher[T]) = enricher

  def create[T, O](f: (T, Int,String) => O) = new HybridRecordEnricher[T] {
    type Out = O
    override def enrichedHybridRecordHList(record: T, version: Int)(s3key: String): Out = f(record,version,s3key)
  }

  object CollectorPoly extends Poly1{
    implicit def some[FT] =
      at[(FT, Some[CopyToDynamo])]{case ((fieldtype, _ )) => fieldtype}
  }

  implicit def enricher[T <: Id, R <: HList,A <: HList, D <: HList, E <: HList, F <: HList](implicit tgen: LabelledGeneric.Aux[T, R], hybridGen: LabelledGeneric.Aux[HybridRecord,F],
                                                                                   annotations: Annotations.Aux[CopyToDynamo, T, A], zipper: Zip.Aux[R :: A :: HNil, D],
                                                                                            collector: Collect.Aux[D, CollectorPoly.type, E], prepend: Prepend[F,E]) = create {
    // This function that takes a record of type T, a version and an s3key and returns a HList
    // with all the fields from HybridRecord plus the fields from record tagged with @CopyToDynamo
    (record: T, version: Int, s3key: String) => {
      val hybridRecord = HybridRecord(record.id, version, s3key)
      val recordAsHlist = tgen.to(record)
      val recordWithAnnotations = recordAsHlist.zip(annotations.apply())
      val taggedFields = recordWithAnnotations.collect(CollectorPoly)
      hybridGen.to(hybridRecord) ::: taggedFields
    }
  }
}