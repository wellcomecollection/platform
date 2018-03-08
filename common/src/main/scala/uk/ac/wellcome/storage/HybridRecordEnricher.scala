package uk.ac.wellcome.storage

import shapeless.ops.hlist.{Collect, Prepend, Zip}
import shapeless.{Id => ShapelessId, _}
import uk.ac.wellcome.models.Id

// Type class that returns a HList with all the fields from HybridRecord,
// plus all the fields from T.
//
// We don't know what the Out class is right now.  Normally types defined
// on traits can only be accessed from an instance, e.g. in
//
//     trait foo { type Bar }
//
// you can only get the type Bar if you have an instance of foo.
//
// Further down, we're using Aux, a Shapeless type class, to get the type
// Out externally -- without needing an instance!
//
trait HybridRecordEnricher[T] {
  type Out
  def enrichedHybridRecordHList(id: String, metadata: T, version: Int)(
    s3key: String): Out
}

object HybridRecordEnricher {
  type Aux[A, R] = HybridRecordEnricher[A] { type Out = R }

  def apply[T](implicit enricher: HybridRecordEnricher[T]) = enricher

  // Create an instance of the HybridRecordEnricher.  This method
  // takes two type parameters:
  //
  //  - M is the type of the metadata, the additional fields that we
  //    add to the HybridRecord.
  //  - O is the type of the combined output class.
  //
  def create[M, O](f: (String, M, Int, String) => O) =
    new HybridRecordEnricher[M] {
      type Out = O
      override def enrichedHybridRecordHList(
        id: String,
        metadata: M,
        version: Int)(s3key: String): Out = f(id, metadata, version, s3key)
    }

  // This method takes three type parameters:
  //
  //  - M for metadata
  //  - LabelledGeneric turns the metadata M into an HList R
  //  - Another LabelledGeneric turns HybridRecord into an HList L
  //
  // Prepend is a Shapeless type class that takes two type parameters,
  // and allows prepending the first to the second.  In this case, it's the
  // two HLists: L (the HybridRecord) and R (the metadata).
  //
  implicit def enricher[M, R <: HList, L <: HList](
    implicit tgen: LabelledGeneric.Aux[M, R],
    hybridGen: LabelledGeneric.Aux[HybridRecord, L],
    prepend: Prepend[L, R]) = create {
    (id: String, metadata: M, version: Int, s3key: String) =>
      {
        val hybridRecord = HybridRecord(id, version, s3key)
        val metadataAsHlist = tgen.to(metadata)

        hybridGen.to(hybridRecord) ::: metadataAsHlist
      }
  }
}
