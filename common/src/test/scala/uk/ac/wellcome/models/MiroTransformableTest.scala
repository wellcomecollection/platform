package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class MiroTransformableTest extends FunSpec with Matchers {

  /** Given all the required data for the transform step, and the expected
   *  final result, assert the transform behaves correctly.
   */
  def assertTransformIsSuccessful(miroID: String, miroCollection: String, data: String, expectedWork: Work) {
    val miroTransformable = MiroTransformable(
      MiroID = miroID,
      MiroCollection = miroCollection,
      data = data
    )

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get shouldBe expectedWork
  }

  /** Given all the required data for the transform step, assert that the
   *  transform fails.
   */
  def assertTransformIsFailure(miroID: String, miroCollection: String, data: String) {
    val miroTransformable = MiroTransformable(
      MiroID = miroID,
      MiroCollection = miroCollection,
      data = data
    )

    miroTransformable.transform.isSuccess shouldBe false
  }

  it("should be able to transform itself into a unified item") {
    assertTransformIsSuccessful(
      miroID = "M0000001",
      miroCollection = "Images-A",
      data = """{"image_title": "A picture of a parrot"}""",
      expectedWork = Work(
        identifiers = List(SourceIdentifier("Miro", "MiroID", "M0000001")),
        label = "A picture of a parrot",
        hasCreatedDate = Some(Period("early 20th century")),
        hasCreator = List(Agent("Henry Wellcome"))
      )
    )
  }

  it("should be able to cope with unrecognised fields in the JSON data") {
    assertTransformIsSuccessful(
      miroID = "M0000002",
      miroCollection = "Images-A",
      data = s"""{"image_title": "A cartoon of a cat", "foo": "bar", "baz": "bat"}""",
      expectedWork = Work(
        identifiers = List(SourceIdentifier("Miro", "MiroID", "M0000002")),
        label = "A cartoon of a cat",
        hasCreatedDate = Some(Period("early 20th century")),
        hasCreator = List(Agent("Henry Wellcome"))
      )
    )
  }

  it("should fail the transform if the JSON is missing the image title") {
    assertTransformIsFailure(
      miroID = "M0000003",
      miroCollection = "Images-A",
      data = """{"not_image_title": 123}"""
    )
  }

  it("should fail the transform if the data field is not valid JSON") {
    assertTransformIsFailure(
      miroID = "M0000004",
      miroCollection = "Images-A",
      data = """Not a valid JSON string, nope."""
    )
  }

  it("should fail the transform if the Miro collection isn't Images-A") {
    assertTransformIsFailure(
      miroID = "M0000005",
      miroCollection = "Images-Z",
      data = """{"image_title": "A drawing of a dog"}"""
    )
  }
}
