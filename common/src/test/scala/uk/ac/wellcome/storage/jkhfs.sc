import shapeless._
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.{CopyToDynamo, HybridRecord}


case class TaggedExampleRecord(id: String,
                               something: String,
                               @CopyToDynamo taggedSomething: String
                              ) extends Id


val taggedExampleRecordGen = LabelledGeneric[TaggedExampleRecord]
val hybridRecordGen = LabelledGeneric[HybridRecord]

val record = TaggedExampleRecord(id = "111", "blah", "important")
val version = 3
val s3key = ""
val hybridRecord = HybridRecord(record.id, version, s3key)
val annotations = Annotations[CopyToDynamo, TaggedExampleRecord].apply()

val repr= taggedExampleRecordGen.to(record)

val value = repr.zip(annotations)

object Collector extends Poly2{
  implicit def some[L <: HList, FT] =
    at[L, (FT, Some[CopyToDynamo])]{case (accumulatorList,(fieldtype, _) ) => fieldtype :: accumulatorList}
implicit def none[L <: HList, FT] =
    at[L, (FT, None.type)]{case (accumulatorList,_) => accumulatorList }
}

val taggedFields = value.foldLeft(HNil: HList)(Collector)

hybridRecordGen.to(hybridRecord) ::: taggedFields
