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
        keywords = Some(List("Animals", "Arachnids", "Fruit"))
      ),
      expectedSubjectLabels = List("Animals", "Arachnids", "Fruit")
    )
  }

  it("uses the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywordsUnauth = Some(List(Some("Altruism"), Some("Mammals")))
      ),
      expectedSubjectLabels = List("Altruism", "Mammals")
    )
  }

  it("uses the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("Humour")),
        keywordsUnauth = Some(List(Some("Marine creatures")))
      ),
      expectedSubjectLabels = List("Humour", "Marine creatures")
    )
  }

  it("normalises subject labels and concepts to sentence case") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("humour", "comedic aspect")),
        keywordsUnauth = Some(List(Some("marine creatures")))
      ),
      expectedSubjectLabels =
        List("Humour", "Comedic aspect", "Marine creatures")
    )
  }

  private def transformRecordAndCheckSubjects(
    miroRecord: MiroRecord,
    expectedSubjectLabels: List[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    val expectedSubjects = expectedSubjectLabels.map { label =>
      Unidentifiable(
        Subject(label = label, concepts = List(Unidentifiable(Concept(label))))
      )
    }
    transformedWork.subjects shouldBe expectedSubjects
  }
}
