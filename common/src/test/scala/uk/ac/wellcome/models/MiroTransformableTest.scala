package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class MiroTransformableTest extends FunSpec with Matchers {
  it("should be able to transform itself into a unified item") {
    val miroId = "123"
    val miroTransformable =
      MiroTransformable(miroId,
                        "Images-A",
                        """{"image_title": "some image title"}""")

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get shouldBe UnifiedItem(
      List(SourceIdentifier("Miro", "MiroID", miroId)),
      Some("some image title"),
      None)
  }

  it("should be able to transform itself into a unified item if the data field is empty") {
    val miroId = "123"
    val miroTransformable =
      MiroTransformable(miroId,
                        "Images-A",
                        """{}""")

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get shouldBe UnifiedItem(
      List(SourceIdentifier("Miro", "MiroID", miroId)),
      None,
      None)
  }
}
