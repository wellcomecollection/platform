package uk.ac.wellcome.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.transformer.transformers.MiroTransformableWrapper

/** Tests that the Miro transformer extracts the "subjects" field correctly.
  *
  *  Although this transformation is currently a bit basic, the data we get
  *  from Miro will need cleaning before it's presented in the API (casing,
  *  names, etc.) -- these tests will become more complicated.
  */
class MiroSubjectsTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("puts an empty subject list on records without keywords") {
    transformRecordAndCheckSubjects(
      data = s""""image_title": "A snail without a subject"""",
      expectedSubjects = List()
    )
  }

  it("uses the image_keywords field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A scorpion with a strawberry",
        "image_keywords": ["animals", "arachnids", "fruit"]
      """,
      expectedSubjects = List(
        Subject(label = "animals", concepts = List(Concept("animals"))),
        Subject(label = "arachnids", concepts = List(Concept("arachnids"))),
        Subject(label = "fruit", concepts = List(Concept("fruit")))
      )
    )
  }

  it("uses the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A sweet seal gives you a sycamore",
        "image_keywords_unauth": ["altruism", "mammals"]
      """,
      expectedSubjects = List(
        Subject(label = "altruism", concepts = List(Concept("altruism"))),
        Subject(label = "mammals", concepts = List(Concept("mammals")))
      )
    )
  }

  it("uses the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A squid, a sponge and a stingray walk into a bar",
        "image_keywords": ["humour"],
        "image_keywords_unauth": ["marine creatures"]
      """,
      expectedSubjects = List(
        Subject(label = "humour", concepts = List(Concept("humour"))),
        Subject(
          label = "marine creatures",
          concepts = List(Concept("marine creatures")))
      )
    )
  }

  private def transformRecordAndCheckSubjects(
    data: String,
    expectedSubjects: List[Subject[AbstractConcept]] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.subjects shouldBe expectedSubjects
  }
}
