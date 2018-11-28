package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

/** Tests that the Miro transformer extracts the "subjects" field correctly.
  *
  *  Although this transformation is currently a bit basic, the data we get
  *  from Miro will need cleaning before it's presented in the API (casing,
  *  names, etc.) -- these tests will become more complicated.
  */
class MiroSubjectsTest
    extends FunSpec
    with Matchers
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("puts an empty subject list on records without keywords") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecord,
      expectedSubjectLabels = List()
    )
  }

  it("uses the image_keywords field if present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("animals", "arachnids", "fruit"))
      ),
      expectedSubjectLabels = List("animals", "arachnids", "fruit")
    )
  }

  it("uses the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywordsUnauth = Some(List(Some("altruism"), Some("mammals")))
      ),
      expectedSubjectLabels = List("altruism", "mammals")
    )
  }

  it("uses the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("humour")),
        keywordsUnauth = Some(List(Some("marine creatures")))
      ),
      expectedSubjectLabels = List("humour", "marine creatures")
    )
  }

  private def transformRecordAndCheckSubjects(
    miroRecord: MiroRecord,
    expectedSubjectLabels: List[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    val expectedSubjects = expectedSubjectLabels.map { label =>
      Subject(label = label, concepts = List(Unidentifiable(Concept(label))))
    }
    transformedWork.subjects shouldBe expectedSubjects
  }
}
