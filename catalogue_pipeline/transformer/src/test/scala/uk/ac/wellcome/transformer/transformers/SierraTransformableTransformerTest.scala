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
    val id = "5757575"
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
        "5151515" -> sierraItemRecord(
          id = "5151515",
          title = title,
          bibIds = List(id)),
        "5252525" -> sierraItemRecord(
          id = "5252525",
          title = title,
          bibIds = List(id))
      )
    )

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)

    transformedSierraRecord.isSuccess shouldBe true
    val work = transformedSierraRecord.get.get

    val sourceIdentifier1 =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "i51515155")
    val sourceIdentifier2 =
      SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "i52525259")

    work.items.map { _.sourceIdentifier } shouldBe List(
      sourceIdentifier1,
      sourceIdentifier2
    )
  }

  it("should extract information from items") {
    val modifiedDate = Instant.now
    val bibId = "6262626"
    val itemId = "6363636"
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
    val expectedSourceIdentifiers = List(
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        value = "i63636360"
      ),
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraIdentifier,
        value = itemId
      )
    )

    work.items.head shouldBe UnidentifiedItem(
      sourceIdentifier = expectedSourceIdentifiers.head,
      identifiers = expectedSourceIdentifiers,
      locations = List(PhysicalLocation(locationType, locationLabel)))
  }

  it("should not perform a transformation without bibData") {
    val sierraTransformable =
      SierraTransformable(sourceId = "0102010", maybeBibData = None)

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get shouldBe None
  }

  it(
    "should not perform a transformation without bibData, even if some itemData is present") {
    val sierraTransformable = SierraTransformable(
      sourceId = "1113111",
      maybeBibData = None,
      itemData = Map(
        "1313131" -> sierraItemRecord(
          id = "1313131",
          title = "An incomplete invocation of items",
          modifiedDate = "2001-01-01T01:01:01Z",
          bibIds = List("1113111")
        ))
    )

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true
    transformedSierraRecord.get shouldBe None
  }

  it("performs a transformation on a work using all varfields") {
    val id = "0606060"
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
          ),
          MarcSubfield(
            tag = "c",
            content = "1923."
          )
        )
      )
    )

    val letteringFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "246",
        indicator1 = " ",
        indicator2 = "6",
        subfields = List(
          MarcSubfield(tag = "a", content = lettering)
        )
      )
    )

    val publishingDateFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "c",
            content = "1923."
          )
        )
      )
    )

    val marcFields = publisherFields ++ descriptionFields ++ letteringFields ++ publishingDateFields

    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
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

    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraSystemNumber,
      value = "b06060602"
    )
    val sierraIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraIdentifier,
      value = id
    )

    transformedSierraRecord.get shouldBe Some(
      UnidentifiedWork(
        title = Some(title),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = List(sourceIdentifier, sierraIdentifier),
        description = Some("A delightful description of a dead daisy."),
        publishers =
          List(Unidentifiable(Organisation(label = "Peaceful Poetry"))),
        lettering = Some(lettering),
        publicationDate = Some(Period("1923."))
      )
    )
  }

  it("makes deleted works invisible") {
    val id = "1789871"
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

    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraSystemNumber,
      value = "b17898717"
    )
    val sierraIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraIdentifier,
      value = id
    )

    transformedSierraRecord.get shouldBe Some(
      UnidentifiedWork(
        title = Some(title),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = List(sourceIdentifier, sierraIdentifier),
        visible = false,
        publicationDate = None)
    )
  }

  it("makes suppressed works invisible") {
    val id = "0001000"
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

    val sourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraSystemNumber,
      value = "b00010005"
    )
    val sierraIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraIdentifier,
      value = id
    )

    transformedSierraRecord.get shouldBe Some(
      UnidentifiedWork(
        title = Some(title),
        sourceIdentifier = sourceIdentifier,
        version = 1,
        identifiers = List(sourceIdentifier, sierraIdentifier),
        visible = false,
        publicationDate = None)
    )
  }

  it("transforms bib records that don't have a title") {
    // This example is taken from a failure observed in the transformer,
    // based on real records from Sierra.
    val id = "2524736"
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

  it("includes the physical description, if present") {
    val id = "7000007"
    val physicalDescription = "A dusty depiction of dodos"

    val data =
      s"""
        | {
        |   "id": "$id",
        |   "title": "Doddering dinosaurs are daring in dance",
        |   "varFields": [
        |     {
        |       "fieldTag": "b",
        |       "marcTag": "300",
        |       "ind1": " ",
        |       "ind2": " ",
        |       "subfields": [
        |         {
        |           "tag": "b",
        |           "content": "$physicalDescription"
        |         }
        |       ]
        |     }
        |   ]
        | }
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get.get.physicalDescription shouldBe Some(
      physicalDescription)
  }

  it("includes the extent, if present") {
    val id = "8000008"
    val extent = "Purple pages"

    val data =
      s"""
        | {
        |   "id": "$id",
        |   "title": "English earwigs earn evidence of evil",
        |   "varFields": [
        |     {
        |       "fieldTag": "a",
        |       "marcTag": "300",
        |       "ind1": " ",
        |       "ind2": " ",
        |       "subfields": [
        |         {
        |           "tag": "a",
        |           "content": "$extent"
        |         }
        |       ]
        |     }
        |   ]
        | }
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get.get.extent shouldBe Some(extent)
  }

  it("includes place of publications") {
    val id = "8008008"
    val place = "Purple pages"

    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "English earwigs earn evidence of evil",
         |   "varFields": [
         |     {
         |       "fieldTag": "a",
         |       "marcTag": "260",
         |       "ind1": " ",
         |       "ind2": " ",
         |       "subfields": [
         |         {
         |           "tag": "a",
         |           "content": "$place"
         |         }
         |       ]
         |     }
         |   ]
         | }
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get.get.placesOfPublication should contain only Place(
      label = place)

  }

  it("includes the work type, if present") {
    val id = "6547529"
    val workTypeId = "xxx"
    val workTypeValue =
      "A parchment of penguin pemmican pierced playfully with pencils."

    val workType = WorkType(
      id = workTypeId,
      label = workTypeValue
    )

    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "Doddering dinosaurs are daring in dance",
         |    "materialType": {
         |      "code": "$workTypeId",
         |      "value": "$workTypeValue"
         |    },
         |   "varFields": []
         | }
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get.get.workType shouldBe Some(workType)
  }

  it("uses the full Sierra system number as the source identifier") {
    val id = "9000009"
    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData = Some(
        SierraBibRecord(
          id = id,
          data = s"""{"id": "$id"}""",
          modifiedDate = now()
        ))
    )

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    val expectedSourceIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.sierraSystemNumber,
      value = "b90000092"
    )

    transformedSierraRecord.get.get.sourceIdentifier
  }

  it("uses the lang for the language field") {
    val id = "1020201"
    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData = Some(
        SierraBibRecord(
          id = id,
          data = s"""{
            "id": "$id",
            "lang": {
              "code": "fra",
              "name": "French"
            }
          }""",
          modifiedDate = now()
        ))
    )

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    val expectedLanguage = Language(
      id = "fra",
      label = "French"
    )

    transformedSierraRecord.get.get.language.get shouldBe expectedLanguage
  }

  it("extracts creators if present") {
    val id = "8008008"
    val name = "Rincewind"

    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "English earwigs earn evidence of evil",
         |   "varFields": [
         |     {
         |       "fieldTag": "",
         |       "marcTag": "100",
         |       "ind1": " ",
         |       "ind2": " ",
         |       "subfields": [
         |         {
         |           "tag": "a",
         |           "content": "$name"
         |         }
         |       ]
         |     }
         |   ]
         | }
      """.stripMargin

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData =
        Some(SierraBibRecord(id = id, data = data, modifiedDate = now())))

    val transformedSierraRecord =
      transformer.transform(sierraTransformable, version = 1)
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get.get.creators should contain only Unidentifiable(
      Person(label = name))
  }
}
