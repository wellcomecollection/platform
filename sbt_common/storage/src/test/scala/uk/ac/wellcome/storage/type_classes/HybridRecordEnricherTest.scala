package uk.ac.wellcome.storage.type_classes

import org.scalatest.{FunSpec, Matchers}
import shapeless.syntax.singleton._
import shapeless.{the => sThe, _}

class HybridRecordEnricherTest extends FunSpec with Matchers {

  case class Metadata(something: String)

  it("""generates a HList with all the fields from HybridRecord
      and extra fields from whatever type passed""") {
    val hybridRecordEnricher = sThe[HybridRecordEnricher[Metadata]]

    val metadata = Metadata("something")

    val id = "1111"
    val version = 3
    val s3Key = "s3Key"
    val hList =
      hybridRecordEnricher.enrichedHybridRecordHList(id, metadata, version)(
        s3Key)

    val expectedHList =
      ("id" ->> id) ::
        ("version" ->> version) ::
        ("s3key" ->> s3Key) ::
        ("something" ->> "something") :: HNil

    hList shouldBe expectedHList
  }
}
