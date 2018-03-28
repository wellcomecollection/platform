package uk.ac.wellcome.transformer.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

/** Tests that the Miro transformer extracts the "subjects" field correctly.
  *
  *  Although this transformation is currently a bit basic, the data we get
  *  from Miro will need cleaning before it's presented in the API (casing,
  *  names, etc.) -- these tests will become more complicated.
  */
class MiroTransformableTransformerSubjectsTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should have an empty subject list on records without keywords") {
    transformRecordAndCheckSubjects(
      data = s""""image_title": "A snail without a subject"""",
      expectedSubjects = List[Concept]()
    )
  }

  it("should use the image_keywords field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A scorpion with a strawberry",
        "image_keywords": ["animals", "arachnids", "fruit"]
      """,
      expectedSubjects = List(
        Concept("animals"),
        Concept("arachnids"),
        Concept("fruit")
      )
    )
  }

  it("should use the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A sweet seal gives you a sycamore",
        "image_keywords_unauth": ["altruism", "mammals"]
      """,
      expectedSubjects = List(
        Concept("altruism"),
        Concept("mammals")
      )
    )
  }

  it(
    "should use the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A squid, a sponge and a stingray walk into a bar",
        "image_keywords": ["humour"],
        "image_keywords_unauth": ["marine creatures"]
      """,
      expectedSubjects = List(
        Concept("humour"),
        Concept("marine creatures")
      )
    )
  }

  it("should create an Item for each Work") {
    val title = "A woodcut of a Weevil"
    val longTitle = "A wonderful woodcut of a weird weevil"
    val descriptionBody = "Woodcut, by A.R. Thropod.  Welsh.  1789."
    val description = s"$longTitle\\n\\n$descriptionBody"
    val work = transformWork(data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """)

    val item = work.items.head

    val identifier =
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        ontologyType = "Item",
        value = "M0000001")

    item shouldBe UnidentifiedItem(
      identifier,
      List(identifier),
      List(
        DigitalLocation(
          locationType = "iiif-image",
          url =
            "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json",
          license = License_CCBY
        )
      )
    )
  }

  private def transformRecordAndCheckSubjects(
    data: String,
    expectedSubjects: List[Concept] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.subjects shouldBe expectedSubjects
  }
}
