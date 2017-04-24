package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class MiroTransformableTest extends FunSpec with Matchers {
  it("should be able to transform itself into a unified item") {
    val miroId = "123"
    val imageTitle = "some image title"
    val miroTransformable =
      MiroTransformable(miroId,
                        "Images-A",
                        s"""{"image_title": "$imageTitle"}""")

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get shouldBe UnifiedItem(
      identifiers = List(SourceIdentifier("Miro", "MiroID", miroId)),
      title = Some(imageTitle))
  }

  it(
    "should be able to transform itself into a unified item if the data fields contains more fields") {
    val miroId = "123"
    val imageTitle = "some image title"
    val miroTransformable =
      MiroTransformable(
        miroId,
        "Images-A",
        s"""{"image_title": "$imageTitle", "image_web_thumb_height": "84", "image_web_thumb_width": "56"}""")

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get shouldBe UnifiedItem(
      identifiers = List(SourceIdentifier("Miro", "MiroID", miroId)),
      title = Some(imageTitle))
  }

  it(
    "should be able to transform itself into a unified item if the data field is empty") {
    val miroId = "123"
    val miroTransformable =
      MiroTransformable(miroId, "Images-A", """{}""")

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get shouldBe UnifiedItem(
      identifiers = List(SourceIdentifier("Miro", "MiroID", miroId)))
  }

  it("should fail transforming itself if the data field is not valid json") {
    val miroId = "123"
    val miroTransformable =
      MiroTransformable(miroId, "Images-A", """not a json string""")

    miroTransformable.transform.isSuccess shouldBe false
  }
}
