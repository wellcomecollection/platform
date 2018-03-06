package uk.ac.wellcome.storage

import shapeless.ops.hlist.{Collect, Prepend, Zip}
import shapeless.{Id => ShapelessId, _}
import uk.ac.wellcome.models.Id

// Type class that returns a HList with all the fields from HybridRecord plus all the fields from T
trait HybridRecordEnricher[T] {
  type Out
  def enrichedHybridRecordHList(id: String, metadata: T, version: Int)(
    s3key: String): Out
}

object HybridRecordEnricher {
  type Aux[A, R] = HybridRecordEnricher[A] { type Out = R }
  def apply[T](implicit enricher: HybridRecordEnricher[T]) = enricher

  def create[T, O](f: (String, T, Int, String) => O) =
    new HybridRecordEnricher[T] {
      type Out = O
      override def enrichedHybridRecordHList(
        id: String,
        metadata: T,
        version: Int)(s3key: String): Out = f(id, metadata, version, s3key)
    }

  implicit def enricher[T, R <: HList, F <: HList](
    implicit tgen: LabelledGeneric.Aux[T, R],
    hybridGen: LabelledGeneric.Aux[HybridRecord, F],
    prepend: Prepend[F, R]) = create {
    (id: String, metadata: T, version: Int, s3key: String) =>
      {
        val hybridRecord = HybridRecord(id, version, s3key)
        val metadataAsHlist = tgen.to(metadata)

        hybridGen.to(hybridRecord) ::: metadataAsHlist
      }
  }
}
