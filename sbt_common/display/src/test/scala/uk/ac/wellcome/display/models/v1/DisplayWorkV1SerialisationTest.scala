package uk.ac.wellcome.display.models.v1

import org.scalatest.FunSpec
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.models.V1WorksIncludes
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.{
  SubjectGenerators,
  WorksGenerators
}
import uk.ac.wellcome.models.work.internal._

class DisplayWorkV1SerialisationTest
    extends FunSpec
    with DisplayV1SerialisationTestBase
    with JsonMapperTestUtil
    with SubjectGenerators
    with WorksGenerators {

  it("serialises a DisplayWorkV1") {
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

    val expectedJson = s"""
       |{
       | "type": "Work",
       | "id": "${work.canonicalId}",
       | "title": "${work.title}",
       | "description": "${work.description.get}",
       | "workType" : ${workType(work.workType.get)},
       | "lettering": "${work.lettering.get}",
       | "createdDate": ${period(work.createdDate.get)},
       | "creators": [ ${identifiedOrUnidentifiable(
                            work.contributors(0).agent,
                            abstractAgent)} ],
       | "subjects": [ ],
       | "genres": [ ],
       | "publishers": [ ],
       | "placesOfPublication": [ ]
       |}
          """.stripMargin

    assertObjectMapsToJson(DisplayWorkV1(work), expectedJson = expectedJson)
  }

  it("renders an item if the items include is present") {
    val work = createIdentifiedWorkWith(
      itemsV1 = createIdentifiedItems(count = 1)
    )

    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "items": [ ${items(work.itemsV1)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
                          |}
          """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(work, V1WorksIncludes(items = true)),
      expectedJson = expectedJson
    )
  }

  it("includes 'items' if the items include is present, even with no items") {
    val work = createIdentifiedWorkWith(
      items = List()
    )
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "items": [ ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
                          |}
          """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(work, V1WorksIncludes(items = true)),
      expectedJson = expectedJson
    )
  }

  it("includes credit information in DisplayWorkV1 serialisation") {
    val location = DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = "",
      credit = Some("Wellcome Collection"),
      license = Some(License_CCBY)
    )
    val item = createIdentifiedItemWith(locations = List(location))
    val workWithCopyright = createIdentifiedWorkWith(
      itemsV1 = List(item)
    )

    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithCopyright.canonicalId}",
                          |     "title": "${workWithCopyright.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ],
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

    assertObjectMapsToJson(
      DisplayWorkV1(workWithCopyright, V1WorksIncludes(items = true)),
      expectedJson = expectedJson
    )
  }

  it("includes subject information in DisplayWorkV1 serialisation") {
    val concept0 = Unidentifiable(Concept("fish"))
    val concept1 = Unidentifiable(Concept("gardening"))

    val workWithSubjects = createIdentifiedWorkWith(
      subjects = List(
        createSubjectWith(concepts = List(concept0)),
        createSubjectWith(concepts = List(concept1))
      )
    )

    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title}",
                          |     "creators": [],
                          |     "subjects": [
                          |       ${concept(concept0.agent)},
                          |       ${concept(concept1.agent)} ],
                          |     "genres": [ ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ]
                          |   }""".stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(workWithSubjects),
      expectedJson = expectedJson
    )
  }

  it("includes genre information in DisplayWorkV1 serialisation") {
    val concept0 = Unidentifiable(Concept("woodwork"))
    val concept1 = Unidentifiable(Concept("etching"))

    val workWithGenres = createIdentifiedWorkWith(
      genres = List(
        Genre("label", List(concept0)),
        Genre("label", List(concept1))
      )
    )
    val expectedJson = s"""
                          |{
                          |     "type": "Work",
                          |     "id": "${workWithGenres.canonicalId}",
                          |     "title": "${workWithGenres.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [
                          |             ${concept(concept0.agent)},
                          |             ${concept(concept1.agent)} ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ]
                          |   }""".stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(workWithGenres),
      expectedJson = expectedJson
    )
  }

  it("includes a list of identifiers on DisplayWorkV1") {
    val otherIdentifier = createSourceIdentifier
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List(otherIdentifier)
    )

    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "identifiers": [ ${identifier(work.sourceIdentifier)}, ${identifier(
                            otherIdentifier)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
                          |}
          """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(work, V1WorksIncludes(identifiers = true)),
      expectedJson = expectedJson
    )
  }

  it(
    "always includes 'identifiers' with the identifiers include, even if there are no extra identifiers") {
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List()
    )

    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "creators": [ ],
                          | "identifiers": [ ${identifier(work.sourceIdentifier)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
                          |}
          """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(work, V1WorksIncludes(identifiers = true)),
      expectedJson = expectedJson
    )
  }

  it(
    "includes the thumbnail field if available and we use the thumbnail include") {
    val work = createIdentifiedWorkWith(
      thumbnail = Some(
        DigitalLocation(
          locationType = LocationType("thumbnail-image"),
          url = "https://iiif.example.org/1234/default.jpg",
          license = Some(License_CCBY)
        ))
    )
    val expectedJson = s"""
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.canonicalId}",
                          |     "title": "${work.title}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ],
                          |     "thumbnail": ${location(work.thumbnail.get)}
                          |   }
          """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV1(work, V1WorksIncludes(thumbnail = true)),
      expectedJson = expectedJson
    )
  }
}
