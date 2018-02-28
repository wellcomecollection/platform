package uk.ac.wellcome.storage

import org.scalatest.{FunSpec, Matchers}
import shapeless.{the => sThe, _}
import shapeless.syntax.singleton._
import uk.ac.wellcome.models.Id

class HybridRecordEnricherTest extends FunSpec with Matchers {

  case class TestRecordWithNoTags(id: String, something: String) extends Id

  it("""generates a HList with all the fields from HybridRecord
      and no extra fields if record doesn't contain tagged fields""") {
    val gen = LabelledGeneric[HybridRecord]
    val hybridRecordEnricher = sThe[HybridRecordEnricher[TestRecordWithNoTags]]

    val id = "1111"
    val version = 3
    val s3Key = "s3Key"
    val hList = hybridRecordEnricher.enrichedHybridRecordHList(
      TestRecordWithNoTags(id, "something"),
      version)(s3Key)

    gen.from(hList) shouldBe HybridRecord(id, version, s3Key)
  }

  case class TestRecordWithTags(id: String,
                                something: String,
                                @CopyToDynamo tagged1: Int,
                                @CopyToDynamo tagged2: String)
      extends Id

  it("""generates a HList with all the fields from HybridRecord
        and all of the tagged fields from record""") {
    val gen = LabelledGeneric[HybridRecord]
    val hybridRecordEnricher = sThe[HybridRecordEnricher[TestRecordWithTags]]

    val id = "1111"
    val version = 3
    val s3Key = "s3Key"
    val taggedInt = 1111
    val taggedString = "taggedString"
    val hList = hybridRecordEnricher.enrichedHybridRecordHList(
      TestRecordWithTags(id, "something", taggedInt, taggedString),
      version)(s3Key)

    val expectedHList =
      ("id" ->> id) ::
        ("version" ->> version) ::
        ("s3key" ->> s3Key) ::
        ("tagged1" ->> taggedInt) ::
        ("tagged2" ->> taggedString) :: HNil

    hList shouldBe expectedHList
  }
}
