package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord, SierraRecordNumber}
import uk.ac.wellcome.models.transformable.sierra.test.utils.{SierraData, SierraUtil}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, VarField}
import uk.ac.wellcome.utils.JsonUtil._

class SierraTransformableTransformerTest
    extends FunSpec
    with Matchers
    with SierraData
    with SierraUtil
    with TransformableTestBase[SierraTransformable]
    with WorksUtil {
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
      sierraId = SierraRecordNumber(id),
      maybeBibData = Some(createSierraBibRecordWith(id = id, data = data)),
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

    val work = transformToWork(sierraTransformable)

    val sourceIdentifier1 =
      SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        "i51515155")
    val sourceIdentifier2 =
      SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        "i52525259")

    work shouldBe a[UnidentifiedWork]

    work.asInstanceOf[UnidentifiedWork].items.map { _.sourceIdentifier } shouldBe List(
      sourceIdentifier1,
      sourceIdentifier2
    )
  }

  it("should extract information from items") {
    val bibId = "6262626"
    val itemId = "6363636"
    val locationType = LocationType("sgmed")
    val locationLabel = "A museum of mermaids"
    val bibData =
      s"""{
            "id": "$bibId",
            "title": "a title"
          }"""
    val itemData =
      s"""{
          |"id": "$itemId",
          |"location": {
          |   "code": "${locationType.id}",
          |   "name": "$locationLabel"
          | }
          |}""".stripMargin

    val transformable = SierraTransformable(
      sourceId = bibId,
      maybeBibData = Some(
        createSierraBibRecordWith(
          id = bibId,
          data = bibData)),
      itemData = Map(
        itemId -> SierraItemRecord(
          id = itemId,
          data = itemData,
          modifiedDate = modifiedDate,
          bibIds = List(bibId),
          unlinkedBibIds = Nil,
          version = 1))
    )

    val work = transformToWork(transformable)
    work shouldBe a[UnidentifiedWork]
    val unidentifiedWork = work.asInstanceOf[UnidentifiedWork]
    unidentifiedWork.items should have size 1

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Item",
      value = "i63636360"
    )

    val expectedOtherIdentifiers = List(
      SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Item",
        value = itemId
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
      maybeBibData = None
    )
  }

  it(
    "does not perform a transformation without bibData, even if some itemData is present") {
    val sierraItemRecord = createSierraItemRecord
    assertTransformReturnsInvisibleWork(
      maybeBibData = None,
      itemData = Map(sierraItemRecord.id -> sierraItemRecord)
    )
  }

  it("performs a transformation on a work using all varfields") {
    val id = "0606060"
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
      value = "b06060602"
    )
    val sierraIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      ontologyType = "Work",
      value = id
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

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedInvisibleWork]
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

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedInvisibleWork]
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

    val work = transformDataToWork(id = id, data = data)
    work shouldBe a[UnidentifiedInvisibleWork]
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.physicalDescription shouldBe Some(physicalDescription)
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.extent shouldBe Some(extent)
  }

  it("includes the work type, if present") {
    val id = "6547529"
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
    val id = "9000009"
    val data = s"""{"id": "$id", "title": "A title"}"""

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      ontologyType = "Work",
      value = "b90000092"
    )

    val work = transformDataToWork(id = id, data = data)
    work.sourceIdentifier shouldBe expectedSourceIdentifier
  }

  it("uses the lang for the language field") {
    val id = "1020201"
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

    val work = transformDataToUnidentifiedWork(id = id, data = data)
    work.contributors shouldBe List(
      Contributor[MaybeDisplayable[AbstractAgent]](
        Unidentifiable(Person(label = name)))
    )
  }

  it("extracts dimensions if present") {
    val id = "9009009"
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
    val id = "9009009"
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
    val id = "9876789"
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
    val id = "9876789"
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
    val id = "2141444"
    val bibData =
      s"""
        |{
        | "id": "$id",
        | "deleted": false,
        | "suppressed": false
        |}
      """.stripMargin
    val bibRecord = createSierraBibRecordWith(id = id, data = bibData)

    assertTransformReturnsInvisibleWork(
      maybeBibData = Some(bibRecord)
    )
  }

  private def transformDataToWork(id: String,
                                  data: String): TransformedBaseWork = {
    val bibRecord = createSierraBibRecordWith(id = id, data = data)

    val sierraTransformable = SierraTransformable(
      sourceId = id,
      maybeBibData = Some(bibRecord)
    )

    transformToWork(sierraTransformable)
  }

  private def assertTransformReturnsInvisibleWork(
    maybeBibData: Option[SierraBibRecord],
    itemData: Map[String, SierraItemRecord] = Map()) = {
    val sierraTransformable = SierraTransformable(
      sourceId = "0102010",
      maybeBibData = maybeBibData
    )

    val triedMaybeWork = transformer.transform(sierraTransformable, version = 1)
    triedMaybeWork.isSuccess shouldBe true

    triedMaybeWork.get shouldBe UnidentifiedInvisibleWork(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Work",
        value = "b01020109"
      ),
      version = 1
    )
  }

  private def transformDataToUnidentifiedWork(
    id: String,
    data: String): UnidentifiedWork = {

    val work = transformDataToWork(id, data)
    work shouldBe a[UnidentifiedWork]
    work.asInstanceOf[UnidentifiedWork]
  }
}
