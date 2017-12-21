package uk.ac.wellcome.transformer.transformers

import java.time.Instant.now

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class SierraTransformableTransformerTest extends FunSpec with Matchers {
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

    val mergedSierraRecord = MergedSierraRecord(
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

    val transformedSierraRecord = transformer.transform(mergedSierraRecord)

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
    val mergedSierraRecord =
      MergedSierraRecord(id = "000", maybeBibData = None)

    val transformedSierraRecord = transformer.transform(mergedSierraRecord)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get shouldBe None
  }

  it(
    "should not perform a transformation without bibData, even if some itemData is present") {
    val mergedSierraRecord = MergedSierraRecord(
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

    val transformedSierraRecord = transformer.transform(mergedSierraRecord)
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

    val mergedSierraRecord = MergedSierraRecord(
      id = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord = transformer.transform(mergedSierraRecord)
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

  def sierraItemRecord(
    id: String = "i111",
    title: String = "Ingenious imps invent invasive implements",
    modifiedDate: String = "2001-01-01T01:01:01Z",
    bibIds: List[String],
    unlinkedBibIds: List[String] = List()
  ) = SierraItemRecord(
    id = id,
    data = sierraRecordString(
      id = id,
      updatedDate = modifiedDate,
      title = title
    ),
    modifiedDate = modifiedDate,
    bibIds = bibIds,
    unlinkedBibIds = unlinkedBibIds
  )

  private def sierraRecordString(
    id: String,
    updatedDate: String,
    title: String
  ) =
    s"""
       |{
       |      "id": "$id",
       |      "updatedDate": "$updatedDate",
       |      "createdDate": "1999-11-01T16:36:51Z",
       |      "deleted": false,
       |      "suppressed": false,
       |      "lang": {
       |        "code": "ger",
       |        "name": "German"
       |      },
       |      "title": "$title",
       |      "author": "Schindler, Rudolf, 1888-",
       |      "materialType": {
       |        "code": "a",
       |        "value": "Books"
       |      },
       |      "bibLevel": {
       |        "code": "m",
       |        "value": "MONOGRAPH"
       |      },
       |      "publishYear": 1923,
       |      "catalogDate": "1999-01-01",
       |      "country": {
       |        "code": "gw ",
       |        "name": "Germany"
       |      }
       |    }
    """.stripMargin
}
