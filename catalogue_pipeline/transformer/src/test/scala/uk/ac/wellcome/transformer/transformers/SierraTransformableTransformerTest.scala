package uk.ac.wellcome.transformer.transformers

import java.time.Instant
import java.time.Instant.now

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  SierraItemData,
  VarField
}
import uk.ac.wellcome.utils.JsonUtil._

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
         | "title": "$title",
         | "varFields": []
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())),
      itemData = Map(
        "i111" -> sierraItemRecord(
          id = "i111",
          title = title,
          bibIds = List(id)),
        "i222" -> sierraItemRecord(
          id = "i222",
          title = title,
          bibIds = List(id))
      )
    )

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)

    transformedSierraRecord.isSuccess shouldBe true
    val work = transformedSierraRecord.get.get

    val sourceIdentifier1 =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "i111")
    val sourceIdentifier2 =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "i222")

    work.items shouldBe List(
      UnidentifiedItem(
        sourceIdentifier = sourceIdentifier1,
        identifiers = List(sourceIdentifier1)
      ),
      UnidentifiedItem(
        sourceIdentifier = sourceIdentifier2,
        identifiers = List(sourceIdentifier2)
      )
    )
  }

  it("should extract information from items") {
    val modifiedDate = Instant.now
    val bibId = "b1234567"
    val itemId = "i1234567"
    val locationType = "sgmed"
    val locationLabel = "A museum of mermaids"
    val bibData =
      s"""{
            "id": "$bibId"
          }"""
    val itemData =
      s"""{
          |"id": "$itemId",
          |"location": {
          |   "code": "$locationType",
          |   "name": "$locationLabel"
          | }
          |}""".stripMargin

    val transformable = SierraTransformable(
      sourceId = bibId,
      maybeBibData = Some(
        SierraBibRecord(
          id = bibId,
          data = bibData,
          modifiedDate = modifiedDate)),
      itemData = Map(
        itemId -> SierraItemRecord(
          id = itemId,
          data = itemData,
          modifiedDate = modifiedDate,
          bibIds = List(bibId),
          unlinkedBibIds = Nil,
          version = 1))
    )

    val triedMaybeWork = transformer.transform(transformable, version = 1)
    triedMaybeWork.isSuccess shouldBe true
    triedMaybeWork.get.isDefined shouldBe true
    val work = triedMaybeWork.get.get
    work.items should have size 1
    val expectedSourceIdentifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, itemId)
    work.items.head shouldBe UnidentifiedItem(
      sourceIdentifier = expectedSourceIdentifier,
      identifiers = List(expectedSourceIdentifier),
      locations = List(PhysicalLocation(locationType, locationLabel)))
  }

  it("should not perform a transformation without bibData") {
    val sierraTransformable =
      SierraTransformable(sourceId = "000", maybeBibData = None)

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get shouldBe None
  }

  it(
    "should not perform a transformation without bibData, even if some itemData is present") {
    val sierraTransformable = SierraTransformable(
      sourceId = "b111",
      maybeBibData = None,
      itemData = Map(
        "i111" -> sierraItemRecord(
          id = "i111",
          title = "An incomplete invocation of items",
          modifiedDate = "2001-01-01T01:01:01Z",
          bibIds = List("b111")
        ))
    )

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true
    transformedSierraRecord.get shouldBe None
  }

  it("performs a transformation on a work using all varfields") {
    val id = "000"
    val title = "Hi Diddle Dee Dee"
    val lettering = "An actor's life for me"

    val publisherFields = List(
      VarField(
        fieldTag = "p",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(tag = "b", content = "Peaceful Poetry")
        )
      )
    )

    val descriptionFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "520",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "a",
            content = "A delightful description of a dead daisy."
          )
        )
      )
    )

    val letteringFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "246",
        indicator1 = "1",
        indicator2 = "6",
        subfields = List()
      )
    )

    val marcFields = publisherFields ++ descriptionFields ++ letteringFields

    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "lettering": "$lettering",
         | "varFields": ${toJson(marcFields).get}
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    val identifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id)

    transformedSierraRecord.get shouldBe Some(
      UnidentifiedWork(
        title = Some(title),
        sourceIdentifier = identifier,
        version = 1,
        identifiers = List(identifier),
        description = Some("A delightful description of a dead daisy."),
        publishers = List(Organisation(label = "Peaceful Poetry")),
        lettering = Some(lettering)
      )
    )
  }

  it("makes deleted works invisible") {
    val id = "000"
    val title = "Hi Diddle Dee Dee"
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "varFields": [],
         | "deleted": true
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    val identifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id)

    transformedSierraRecord.get shouldBe Some(
      UnidentifiedWork(
        title = Some(title),
        sourceIdentifier = identifier,
        version = 1,
        identifiers = List(identifier),
        visible = false)
    )
  }

  it("makes supressed works invisible") {
    val id = "000"
    val title = "Hi Diddle Dee Dee"
    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "varFields": [],
         | "suppressed": true
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    val identifier =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id)

    transformedSierraRecord.get shouldBe Some(
      UnidentifiedWork(
        title = Some(title),
        sourceIdentifier = identifier,
        version = 1,
        identifiers = List(identifier),
        visible = false)
    )
  }

  it("transforms bib records that don't have a title") {
    // This example is taken from a failure observed in the transformer, based on
    // real records from Sierra.
    val id = "b2524736"
    val data =
      s"""
         |{
         |  "id": "$id",
         |  "deletedDate": "2017-02-20",
         |  "deleted": true,
         |  "orders": [],
         |  "locations": [],
         |  "fixedFields": {},
         |  "varFields": []
         |}
        """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get.get.title shouldBe None
  }
}
