package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.V2WorksIncludes
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayWorkV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

  it("serialises a DisplayWorkV2") {
    val work = createIdentifiedWorkWith(
      workType = Some(
        WorkType(id = randomAlphanumeric(5), label = randomAlphanumeric(10))),
      description = Some(randomAlphanumeric(100)),
      lettering = Some(randomAlphanumeric(100)),
      createdDate = Some(Period("1901")),
      contributors = List(
        Contributor(Unidentifiable(Agent(randomAlphanumeric(25))))
      )
    )

    val actualJsonString = objectMapper.writeValueAsString(DisplayWorkV2(work))

    val expectedJsonString = s"""
       |{
       | "type": "Work",
       | "id": "${work.canonicalId}",
       | "title": "${work.title}",
       | "description": "${work.description.get}",
       | "workType" : ${workType(work.workType.get)},
       | "lettering": "${work.lettering.get}",
       | "createdDate": ${period(work.createdDate.get)},
       | "contributors": [ ${contributor(work.contributors.head)} ],
       | "genres": [ ],
       | "production": [ ]
       |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJsonString, expectedJsonString)
  }

  it("renders an item if the items include is present") {
    val work = createIdentifiedWorkWith(
      items = createIdentifiedItems(count = 1) :+ createUnidentifiableItemWith()
    )

    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, V2WorksIncludes(items = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "items": [ ${items(work.items)} ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes 'items' if the items include is present, even with no items") {
    val work = createIdentifiedWorkWith(
      items = List()
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, V2WorksIncludes(items = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "items": [ ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes credit information in DisplayWorkV2 serialisation") {
    val location = DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = "",
      credit = Some("Wellcome Collection"),
      license = Some(License_CCBY)
    )
    val item = createIdentifiedItem(locations = List(location))
    val workWithCopyright = createIdentifiedWorkWith(
      items = List(item)
    )

    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(workWithCopyright, V2WorksIncludes(items = true)))
    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithCopyright.canonicalId}",
                          |     "title": "${workWithCopyright.title}",
                          |     "contributors": [ ],
                          |     "genres": [ ],
                          |     "production": [ ],
                          |     "items": [
                          |       {
                          |         "id": "${item.canonicalId}",
                          |         "type": "${item.agent.ontologyType}",
                          |         "locations": [
                          |           {
                          |             "type": "${location.ontologyType}",
                          |             "url": "",
                          |             "locationType": ${locationType(
                            location.locationType)},
                          |             "license": ${license(
                            location.license.get)},
                          |             "credit": "${location.credit.get}"
                          |           }
                          |         ]
                          |       }
                          |     ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes subject information in DisplayWorkV2 serialisation with the subjects include") {
    val workWithSubjects = createIdentifiedWorkWith(
      subjects = List(
        Subject("label", List(Unidentifiable(Concept("fish")))),
        Subject("label", List(Unidentifiable(Concept("gardening"))))
      )
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWorkV2(workWithSubjects, includes = V2WorksIncludes(subjects = true)))
    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title}",
                          |     "contributors": [],
                          |     "subjects": [ ${subjects(
                            workWithSubjects.subjects)} ],
                          |     "genres": [ ],
                          |     "production": [ ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes genre information in DisplayWorkV2 serialisation") {
    val workWithSubjects = createIdentifiedWorkWith(
      genres = List(
        Genre(
          label = "genre",
          concepts = List(
            Unidentifiable(Concept("woodwork")),
            Unidentifiable(Concept("etching"))
          )
        )
      )
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWorkV2(workWithSubjects))
    val expectedJson = s"""
                          |{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title}",
                          |     "contributors": [],
                          |     "genres": [ ${genres(workWithSubjects.genres)} ],
                          |     "production": [ ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes a list of identifiers on DisplayWorkV2") {
    val otherIdentifier = createSourceIdentifier
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List(otherIdentifier)
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, V2WorksIncludes(identifiers = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "identifiers": [ ${identifier(work.sourceIdentifier)}, ${identifier(
                            otherIdentifier)} ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin
    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("always includes 'identifiers' with the identifiers include") {
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List()
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, V2WorksIncludes(identifiers = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "identifiers": [ ${identifier(work.sourceIdentifier)} ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin
    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("shows the thumbnail field if available") {
    val work = createIdentifiedWorkWith(
      thumbnail = Some(
        DigitalLocation(
          locationType = LocationType("thumbnail-image"),
          url = "https://iiif.example.org/1234/default.jpg",
          license = Some(License_CCBY)
        ))
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWorkV2(work, V2WorksIncludes()))
    val expectedJson = s"""
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.canonicalId}",
                          |     "title": "${work.title}",
                          |     "contributors": [ ],
                          |     "genres": [ ],
                          |     "production": [ ],
                          |     "thumbnail": ${location(work.thumbnail.get)}
                          |   }
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }
}
