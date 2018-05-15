package uk.ac.wellcome.platform.transformer.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.DigitalLocation

class MiroTransformableTransformerCopyrightTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("should have no credit line if there's not enough information") {
    transformRecordAndCheckCredit(
      data = s""""image_title": "An image without any copyright?""""
    )
  }

  it("should use the image_credit_line field if present") {
    transformRecordAndCheckCredit(
      data = s"""
        "image_title": "A tumultuous transformation of trees",
        "image_credit_line": "Wellcome Collection"
      """,
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("should use the image_credit_line in preference to image_source_code") {
    transformRecordAndCheckCredit(
      data = s"""
        "image_title": "A tumultuous transformation of trees",
        "image_credit_line": "Wellcome Collection",
        "image_source_code": "CAM"
      """,
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("should use image_source_code if image_credit_line is empty") {
    transformRecordAndCheckCredit(
      data = s"""
        "image_title": "A tumultuous transformation of trees",
        "image_credit_line": null,
        "image_source_code": "CAM"
      """,
      expectedCredit = Some("Benedict Campbell")
    )
  }

  it("should use the uppercased version of the source_code if necessary") {
    transformRecordAndCheckCredit(
      data = s"""
        "image_title": "A loud and leafy lime",
        "image_source_code": "wel"
      """,
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("should tidy up the credit line if necessary") {
    transformRecordAndCheckCredit(
      data = s"""
        "image_title": "Outside an odorous oak",
        "image_credit_line": "The Wellcome Library, London"
      """,
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("should correctly handle special characters in the contributor map") {
    transformRecordAndCheckCredit(
      data = s"""
        "image_title": "A fanciful flurry of firs",
        "image_credit_line": null,
        "image_source_code": "FEI"
      """,
      expectedCredit = Some("Fernán Federici")
    )
  }

  private def transformRecordAndCheckCredit(
    data: String,
    expectedCredit: Option[String] = None
  ) = {
    val transformedWork = transformWork(data = data)
    val location = transformedWork.items.head.locations.head
    location shouldBe a[DigitalLocation]
    location.asInstanceOf[DigitalLocation].credit shouldBe expectedCredit
  }
}
