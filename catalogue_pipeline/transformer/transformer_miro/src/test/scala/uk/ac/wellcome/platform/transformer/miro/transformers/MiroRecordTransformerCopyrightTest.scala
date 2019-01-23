package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.DigitalLocation
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

class MiroRecordTransformerCopyrightTest
    extends FunSpec
    with Matchers
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("has no credit line if there's not enough information") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecord,
      expectedCredit = None
    )
  }

  it("uses the image_credit_line field if present") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = Some("Wellcome Collection")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("uses the image_credit_line in preference to image_source_code") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = Some("Wellcome Collection"),
        sourceCode = Some("CAM")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("uses image_source_code if image_credit_line is empty") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = None,
        sourceCode = Some("CAM")
      ),
      expectedCredit = Some("Benedict Campbell")
    )
  }

  it("uses the uppercased version of the source_code if necessary") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        sourceCode = Some("wel")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("tidies up the credit line if necessary") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = Some("The Wellcome Library, London")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("handles special characters in the contributor map") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        sourceCode = Some("FEI")
      ),
      expectedCredit = Some("Fernán Federici")
    )
  }

  private def transformRecordAndCheckCredit(
    miroRecord: MiroRecord,
    expectedCredit: Option[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    val location = transformedWork.itemsV1.head.agent.locations.head
    location shouldBe a[DigitalLocation]
    location.asInstanceOf[DigitalLocation].credit shouldBe expectedCredit
  }
}
