package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}

class MiroTransformableTest extends FunSpec with Matchers {

  private def transformMiroRecord(
    miroID: String = "M0000001",
    miroCollection: String = "TestCollection",
    data: String = """{"image_title": "A test tome about a tapir"}"""
  ): Work = {
    val miroTransformable = MiroTransformable(
      MiroID = miroID,
      MiroCollection = miroCollection,
      data = data
    )

    miroTransformable.transform.isSuccess shouldBe true
    miroTransformable.transform.get
  }

  it("should pass through the Miro identifier") {
    val miroID = "M0000005_test"
    val work = transformMiroRecord(miroID = miroID)
    work.identifiers shouldBe List(SourceIdentifier("Miro", "MiroID", miroID))
  }

  it("should pass through the image_title to the label field") {
    val title = "A picture of a parrot"
    val work = transformMiroRecord(data = s"""{"image_title": "$title"}""")
    work.label shouldBe title
  }

  it("should have an empty list if no image_creator field is present") {
    val work = transformMiroRecord(data = s"""{"image_title": "A guide to giraffes"}""")
    work.hasCreator shouldBe List[Agent]()
  }

  it("should have an empty list if the image_creator field is empty") {
    val work = transformMiroRecord(data = s"""{"image_title": "A box of beavers", "image_creator": []}""")
    work.hasCreator shouldBe List[Agent]()
  }

  it("should pass through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    val work = transformMiroRecord(
      data = s"""{"image_title": "A radio for a racoon", "image_creator": ["$creator"]}"""
    )
    work.hasCreator shouldBe List(Agent(creator))
  }

  it("should pass through multiple values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Cat-wrangler Carol"
    val creator3 = "Dog-owner Derek"
    val work = transformMiroRecord(
      data = s"""{"image_title": "A book about badgers", "image_creator": ["$creator1", "$creator2", "$creator3"]}"""
    )
    work.hasCreator shouldBe List(Agent(creator1), Agent(creator2), Agent(creator3))
  }

  it("should have no description if no image_image_desc field is present") {
    val work = transformMiroRecord(data = s"""{"image_title": "A line of lions"}""")
    work.description shouldBe None
  }

  it("should pass through the value of the description field") {
    val description = "A new novel about northern narwhals in November"
    val work = transformMiroRecord(
      data = s"""{"image_title": "A note on narwhals", "image_image_desc": "$description"}"""
    )
    work.description shouldBe Some(description)
  }
}
