package uk.ac.wellcome.transformer.transformers

import java.time.Instant.now

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.test.utils.SierraData

class SierraTransformableTransformerTest
    extends FunSpec
    with Matchers
    with SierraData {
  val transformer = new SierraTransformableTransformer

  it("performs a transformation on a work with items") {
    val id = "b5757575"
    val title = "A morning mixture of molasses and muesli"
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title"
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      id = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())),
      itemData = Map(
        "i111" -> sierraItemRecord(id = "i111",
                                   title = title,
                                   bibIds = List(id)),
        "i222" -> sierraItemRecord(id = "i222",
                                   title = title,
                                   bibIds = List(id))
      )
    )

    val transformedSierraRecord = transformer.transform(sierraTransformable)

    transformedSierraRecord.isSuccess shouldBe true
    val work = transformedSierraRecord.get.get

    val sourceIdentifier1 =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "i111")
    val sourceIdentifier2 =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "i222")

    work.items shouldBe List(
      Item(
        sourceIdentifier = sourceIdentifier1,
        identifiers = List(sourceIdentifier1)
      ),
      Item(
        sourceIdentifier = sourceIdentifier2,
        identifiers = List(sourceIdentifier2)
      )
    )
  }

  it("should not perform a transformation without bibData") {
    val sierraTransformable =
      SierraTransformable(id = "000", maybeBibData = None)

    val transformedSierraRecord = transformer.transform(sierraTransformable)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get shouldBe None
  }

  it(
    "should not perform a transformation without bibData, even if some itemData is present") {
    val sierraTransformable = SierraTransformable(
      id = "b111",
      maybeBibData = None,
      itemData = Map(
        "i111" -> sierraItemRecord(
          id = "i111",
          title = "An incomplete invocation of items",
          modifiedDate = "2001-01-01T01:01:01Z",
          bibIds = List("b111")
        ))
    )

    val transformedSierraRecord = transformer.transform(sierraTransformable)
    transformedSierraRecord.isSuccess shouldBe true
    transformedSierraRecord.get shouldBe None
  }

  it("should transform itself into a work") {
    val id = "000"
    val title = "Hi Diddle Dee Dee"
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title"
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      id = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord = transformer.transform(sierraTransformable)
    transformedSierraRecord.isSuccess shouldBe true

    val identifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id)

    transformedSierraRecord.get shouldBe Some(
      Work(
        title = title,
        sourceIdentifier = identifier,
        identifiers = List(identifier)
      )
    )
  }
}
