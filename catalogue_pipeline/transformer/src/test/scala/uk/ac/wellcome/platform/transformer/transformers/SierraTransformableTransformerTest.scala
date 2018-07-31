package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibNumber,
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, VarField}
import uk.ac.wellcome.json.JsonUtil._

class SierraTransformableTransformerTest
    extends FunSpec
    with Matchers
    with SierraUtil
    with TransformableTestBase[SierraTransformable]
    with WorksUtil {
  val transformer = new SierraTransformableTransformer

  it("performs a transformation on a work with physical items") {
    val itemRecords = List(
      createSierraItemRecord,
      createSierraItemRecord
    )

    val sierraTransformable = createSierraTransformableWith(
      maybeBibRecord = Some(createSierraBibRecord),
      itemRecords = itemRecords
    )

    val expectedIdentifiers = itemRecords.map { record =>
      SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = record.id.withCheckDigit
      )
    }

    val work = transformToWork(sierraTransformable)

    work shouldBe a[UnidentifiedWork]

    val actualIdentifiers = work
      .asInstanceOf[UnidentifiedWork]
      .items
      .map { _.asInstanceOf[Identifiable[Item]].sourceIdentifier }

    actualIdentifiers should contain theSameElementsAs expectedIdentifiers
  }

  it("extracts information from items") {
    val bibId = createSierraBibNumber
    val itemId = createSierraItemNumber
    val locationType = LocationType("sgmed")
    val locationLabel = "A museum of mermaids"
    val itemData =
      s"""
         |{
         |  "id": "$itemId",
         |  "location": {
         |    "code": "${locationType.id}",
         |    "name": "$locationLabel"
         |  }
         |}
         |""".stripMargin

    val itemRecord = createSierraItemRecordWith(
      id = itemId,
      data = itemData,
      bibIds = List(bibId)
    )

    val bibRecord = createSierraBibRecordWith(id = bibId)

    val transformable = createSierraTransformableWith(
      sierraId = bibId,
      maybeBibRecord = Some(bibRecord),
      itemRecords = List(itemRecord)
    )

    val work = transformToWork(transformable)
    work shouldBe a[UnidentifiedWork]
    val unidentifiedWork = work.asInstanceOf[UnidentifiedWork]
    unidentifiedWork.items should have size 1

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Item",
      value = itemId.withCheckDigit
    )

    val expectedOtherIdentifiers = List(
      SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Item",
        value = itemId.withoutCheckDigit
      )
    )

    unidentifiedWork.items.head shouldBe Identifiable(
      sourceIdentifier = expectedSourceIdentifier,
      otherIdentifiers = expectedOtherIdentifiers,
      agent =
        Item(locations = List(PhysicalLocation(locationType, locationLabel)))
    )
  }

  it("returns an InvisibleWork if there isn't any bib data") {
    assertTransformReturnsInvisibleWork(
      maybeBibRecord = None
    )
  }

  it(
    "should not perform a transformation without bibData, even if some itemData is present") {
    assertTransformReturnsInvisibleWork(
      maybeBibRecord = None,
      itemRecords = List(createSierraItemRecord)
    )
  }

  it("performs a transformation on a work using all varfields") {
    val id = createSierraBibNumber
    val title = "Hi Diddle Dee Dee"
    val lettering = "An actor's life for me"

    val productionFields = List(
      VarField(
        fieldTag = "p",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(tag = "b", content = "Peaceful Poetry"),
          MarcSubfield(tag = "c", content = "1923.")
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

    val marcFields = productionFields ++ descriptionFields ++ letteringFields

    val data =
      s"""
         |{
         | "id": "$id",
         | "title": "$title",
         | "varFields": ${toJson(marcFields).get}
         |}
        """.stripMargin

    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Work",
      value = id.withCheckDigit
    )
    val sierraIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      ontologyType = "Work",
      value = id.withoutCheckDigit
    )

    val work = transformDataToWork(id = id, data = data)

    work shouldBe createUnidentifiedWorkWith(
      title = title,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = List(sierraIdentifier),
      description = Some("A delightful description of a dead daisy."),
      production = List(
        ProductionEvent(
          places = List(),
          agents = List(Unidentifiable(Agent(label = "Peaceful Poetry"))),
          dates = List(Period("1923.")),
          function = None
        )
      ),
      lettering = Some(lettering)
    )
  }

  it("makes deleted works invisible") {
    val id = createSierraBibNumber
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

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedInvisibleWork]
  }

  it("makes suppressed works invisible") {
    val id = createSierraBibNumber
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

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedInvisibleWork]
  }

  it("transforms bib records that don't have a title") {
    // This example is taken from a failure observed in the transformer,
    // based on real records from Sierra.
    val id = createSierraBibNumber
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

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedInvisibleWork]
  }

  it("includes the physical description, if present") {
    val id = createSierraBibNumber
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.physicalDescription shouldBe Some(physicalDescription)
  }

  it("includes the extent, if present") {
    val id = createSierraBibNumber
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.extent shouldBe Some(extent)
  }

  it("includes the work type, if present") {
    val id = createSierraBibNumber
    val workTypeId = "xxx"
    val workTypeValue =
      "A parchment of penguin pemmican pierced playfully with pencils."

    val expectedWorkType = WorkType(
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.workType shouldBe Some(expectedWorkType)
  }

  it("uses the full Sierra system number as the source identifier") {
    val id = createSierraBibNumber
    val data = s"""{"id": "$id", "title": "A title"}"""

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Work",
      value = id.withCheckDigit
    )

    val work = transformDataToWork(id = id, data = data)
    work.sourceIdentifier shouldBe expectedSourceIdentifier
  }

  it("uses the lang for the language field") {
    val id = createSierraBibNumber
    val data =
      s"""{
         |  "id": "$id",
         |  "lang": {
         |    "code": "fra",
         |    "name": "French"
         |  },
         |  "title": "A title"
         |}""".stripMargin

    val expectedLanguage = Language(
      id = "fra",
      label = "French"
    )

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.language.get shouldBe expectedLanguage
  }

  it("extracts contributor information if present") {
    val id = createSierraBibNumber
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.contributors shouldBe List(
      Contributor[MaybeDisplayable[AbstractAgent]](
        Unidentifiable(Person(label = name)))
    )
  }

  it("extracts dimensions if present") {
    val id = createSierraBibNumber
    val dimensions = "24cm"

    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "Dastardly Danish dogs draw dubious doughnuts",
         |   "varFields": [
         |     {
         |       "fieldTag": "",
         |       "marcTag": "300",
         |       "ind1": " ",
         |       "ind2": " ",
         |       "subfields": [
         |         {
         |           "tag": "c",
         |           "content": "$dimensions"
         |         }
         |       ]
         |     }
         |   ]
         | }
      """.stripMargin

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.dimensions shouldBe Some(dimensions)
  }

  it("extracts subjects if present") {
    val id = createSierraBibNumber
    val content = "A content"

    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "Dastardly Danish dogs draw dubious doughnuts",
         |   "varFields": [
         |     {
         |       "fieldTag": "",
         |       "marcTag": "650",
         |       "ind1": " ",
         |       "ind2": " ",
         |       "subfields": [
         |         {
         |           "tag": "a",
         |           "content": "$content"
         |         }
         |       ]
         |     }
         |   ]
         | }
      """.stripMargin

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.subjects shouldBe List(
      Subject(content, List(Unidentifiable(Concept(content)))))
  }

  it("adds production events if possible") {
    val id = createSierraBibNumber
    val placeLabel = "London"

    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "Loosely lamenting the lemons of London",
         |   "varFields": [
         |     {
         |       "fieldTag": "",
         |       "marcTag": "260",
         |       "ind1": " ",
         |       "ind2": " ",
         |       "subfields": [
         |         {
         |           "tag": "a",
         |           "content": "$placeLabel"
         |         }
         |       ]
         |     }
         |   ]
         | }
      """.stripMargin

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.production shouldBe List(
      ProductionEvent(
        places = List(Place(placeLabel)),
        agents = List(),
        dates = List(),
        function = None
      )
    )
  }

  it("extracts merge candidates from 776 subfield $$w") {
    val id = createSierraBibNumber
    val mergeCandidateBibNumber = "b21414440"
    val data =
      s"""
         | {
         |   "id": "$id",
         |   "title": "Loosely lamenting the lemons of London",
         |   "varFields": [
         |     {
         |       "fieldTag": "",
         |       "marcTag": "776",
         |       "ind1": " ",
         |       "ind2": " ",
         |       "subfields": [
         |         {
         |           "tag": "w",
         |           "content": "(UkLW)$mergeCandidateBibNumber"
         |         }
         |       ]
         |     }
         |   ]
         | }
      """.stripMargin

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.mergeCandidates shouldBe List(
      MergeCandidate(
        SourceIdentifier(
          IdentifierType("sierra-system-number"),
          "Work",
          mergeCandidateBibNumber)))
  }

  it("returns an InvisibleWork if bibData has no title") {
    val id = createSierraBibNumber
    val bibData =
      s"""
        |{
        | "id": "$id",
        | "deleted": false,
        | "suppressed": false
        |}
      """.stripMargin
    val bibRecord = createSierraBibRecordWith(
      id = id,
      data = bibData
    )

    assertTransformReturnsInvisibleWork(
      maybeBibRecord = Some(bibRecord)
    )
  }

  private def transformDataToWork(id: SierraBibNumber,
                                  data: String): TransformedBaseWork = {
    val bibRecord = createSierraBibRecordWith(
      id = id,
      data = data
    )

    val sierraTransformable = SierraTransformable(
      bibRecord = bibRecord
    )

    transformToWork(sierraTransformable)
  }

  private def assertTransformReturnsInvisibleWork(
    maybeBibRecord: Option[SierraBibRecord],
    itemRecords: List[SierraItemRecord] = List()) = {
    val id = createSierraBibNumber

    val sierraTransformable = createSierraTransformableWith(
      sierraId = id,
      maybeBibRecord = maybeBibRecord,
      itemRecords = itemRecords
    )

    val triedMaybeWork = transformer.transform(sierraTransformable, version = 1)
    triedMaybeWork.isSuccess shouldBe true

    triedMaybeWork.get shouldBe UnidentifiedInvisibleWork(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Work",
        value = id.withCheckDigit
      ),
      version = 1
    )
  }

  private def transformDataToUnidentifiedWork(
    id: SierraBibNumber,
    data: String): UnidentifiedWork = {

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedWork]
    work.asInstanceOf[UnidentifiedWork]
  }
}
