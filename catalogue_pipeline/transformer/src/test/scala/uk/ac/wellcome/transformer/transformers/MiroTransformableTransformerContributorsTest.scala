package uk.ac.wellcome.transformer.transformers

import org.scalatest.{FunSpec, FunSuite}
import uk.ac.wellcome.models.{Agent, Contributor, Unidentifiable}

class MiroTransformableTransformerContributorsTest
    extends FunSpec
    with MiroTransformableWrapper {
  it("if not image_creator field is present") {
    transformRecordAndCheckContributors(
      data = s""""image_title": "A guide to giraffes"""",
      expectedContributors = List()
    )
  }

  it("passes through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_creator": ["$creator"]
        """,
      expectedContributors = List(creator)
    )
  }

  it("ignores null values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Dog-owner Derek"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_creator": ["$creator1", null, "$creator2"]
        """,
      expectedContributors = List(creator1, creator2)
    )
  }

  it("passes through multiple values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Cat-wrangler Carol"
    val creator3 = "Dog-owner Derek"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_creator": ["$creator1", "$creator2", "$creator3"]
        """,
      expectedContributors = List(creator1, creator2, creator3)
    )
  }

  it("passes through a single value in the image_creator_secondary field") {
    val secondaryCreator = "Scientist Sarah"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_secondary_creator": ["$secondaryCreator"]
        """,
      expectedContributors = List(secondaryCreator)
    )
  }

  it("passes through multiple values in the image_creator_secondary field") {
    val secondaryCreator1 = "Gamekeeper Gordon"
    val secondaryCreator2 = "Herpetologist Harriet"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "Verdant and vivid",
          "image_secondary_creator": [
            "$secondaryCreator1", "$secondaryCreator2"
          ]
        """,
      expectedContributors = List(secondaryCreator1, secondaryCreator2)
    )
  }

  it("combines the image_creator and image_secondary_creator fields") {
    val creator = "Mycologist Morgan"
    val secondaryCreator = "Manufacturer Mel"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "Verdant and vivid",
          "image_creator": ["$creator"],
          "image_secondary_creator": ["$secondaryCreator"]
        """,
      expectedContributors = List(creator, secondaryCreator)
    )
  }

  it("passes through a value from the image_source_code field") {
    transformRecordAndCheckContributors(
      data = """
          "image_title": "A gander and a goose are game for a goof",
          "image_source_code": "GAV"
        """,
      expectedContributors = List("Isabella Gavazzi")
    )
  }

  it("does not use the image_source_code field for Wellcome Collection") {
    transformRecordAndCheckContributors(
      data = """
          "image_title": "Wandering wallabies within water",
          "image_source_code": "WEL"
        """,
      expectedContributors = List()
    )
  }

  it("combines the image_creator and image_source_code fields") {
    val creator = "Sally Snake"
    transformRecordAndCheckContributors(
      data = s"""
          "image_title": "A gander and a goose are game for a goof",
          "image_creator": ["$creator"],
          "image_source_code": "SNL"
        """,
      expectedContributors = List(creator, "Sue Snell")
    )
  }

  private def transformRecordAndCheckContributors(
    data: String,
    expectedContributors: List[String]
  ) = {
    val transformedWork = transformWork(data = data)
    transformedWork.contributors shouldBe expectedContributors.map { contributor =>
      Contributor(agent = Unidentifiable(Agent(creator)))
    }
  }
}
