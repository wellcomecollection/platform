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

  it("passes through the title from the bib record") {
    val id = "t1234"
    val title = "Tickling a tiny turtle in Tenerife"
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title"
         |}
        """.stripMargin

    val mergedSierraRecord = MergedSierraRecord(
      bibRecord = SierraBibRecord(id = id, data = data, modifiedDate = now())
    )

    val transformedSierraRecord = transformer.transform(mergedSierraRecord)
    transformedSierraRecord.isSuccess shouldBe true
    val work = transformedSierraRecord.get.get

    work.title shouldBe title
  }

  describe("publishers") {
    it("picks up zero publishers") {
      assertPublisherJsonGivesExpectedPublishers(
        json = "", expectedPublishers = List()
      )
    }

    it("ignores information unrelated to the name of the publisher") {
      assertPublisherJsonGivesExpectedPublishers(
        json = """
          "varFields": [
            {
              "fieldTag": "p",
              "marcTag": "260",
              "ind1": " ",
              "ind2": " ",
              "subfields": [
                {
                  "tag": "c",
                  "content": "1984"
                }
              ]
            }
          ],
        """.stripMargin,
        expectedPublishers = List()
      )
    }

    it("picks up information about the name of the publisher") {
      assertPublisherJsonGivesExpectedPublishers(
        json = """
          "varFields": [
            {
              "fieldTag": "p",
              "marcTag": "260",
              "ind1": " ",
              "ind2": " ",
              "subfields": [
                {
                  "tag": "b",
                  "content": "H. Humphrey"
                }
              ]
            }
          ],
        """.stripMargin,
        expectedPublishers = List(Agent(
          label = "H. Humphrey",
          ontologyType = "Organisation"
        ))
      )
    }

    it("picks up information about multiple publishers") {
      // Based on an example in
      // http://www.loc.gov/marc/bibliographic/bd260.html
      assertPublisherJsonGivesExpectedPublishers(
        json = """
          "varFields": [
            {
              "fieldTag": "p",
              "marcTag": "260",
              "ind1": " ",
              "ind2": " ",
              "subfields": [
                {
                  "tag": "a",
                  "content": "Paris"
                },
                {
                  "tag": "b",
                  "content": "Gauthier-Villars"
                },
                {
                  "tag": "a",
                  "content": "Chicago"
                },
                {
                  "tag": "b",
                  "content": "University of Chicago Press"
                },
                {
                  "tag": "c",
                  "content": "1955"
                }
              ]
            }
          ],
        """.stripMargin,
        expectedPublishers = List(
          Agent(
            label = "H. Humphrey",
            ontologyType = "Organisation"
          ),
          Agent(
            label = "University of Chicago Press",
            ontologyType = "Organisation"
          )
        )
      )
    }

  }

  private def assertPublisherJsonGivesExpectedPublishers(
    json: String, expectedPublishers: List[Agent]
  ) = {
    val data = s"""{
      $json
      "id": "p1234",
      "title": "A pack of puffins"
    }"""

    val mergedSierraRecord = MergedSierraRecord(
      bibRecord = SierraBibRecord(
        id = "p1234",
        data = data,
        modifiedDate = now()
      )
    )

    val transformedSierraRecord = transformer.transform(mergedSierraRecord)
    transformedSierraRecord.isSuccess shouldBe true
    val work = transformedSierraRecord.get.get

    work.publishers shouldBe expectedPublishers
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
