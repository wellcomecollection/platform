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
      expectedSubjectLabels = List("animals", "arachnids", "fruit")
    )
  }

  it("uses the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A sweet seal gives you a sycamore",
        "image_keywords_unauth": ["altruism", "mammals"]
      """,
      expectedSubjectLabels = List("altruism", "mammals")
    )
  }

  it(
    "uses the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      data = s"""
        "image_title": "A squid, a sponge and a stingray walk into a bar",
        "image_keywords": ["humour"],
        "image_keywords_unauth": ["marine creatures"]
      """,
      expectedSubjectLabels = List("humour", "marine creatures")
    )
  }

  private def transformRecordAndCheckSubjects(
    data: String,
    expectedSubjectLabels: List[String] = List()
  ) = {
    val transformedWork = transformWork(data = data)
    val expectedSubjects = expectedSubjectLabels.map { label =>
      Subject(label = label, concepts = List(Concept(label)))
    }
    transformedWork.subjects shouldBe expectedSubjects
  }
}
