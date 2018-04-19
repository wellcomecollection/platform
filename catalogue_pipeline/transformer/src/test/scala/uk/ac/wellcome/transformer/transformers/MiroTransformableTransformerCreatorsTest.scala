package uk.ac.wellcome.transformer.transformers

import org.scalatest.{FunSpec, FunSuite}
import uk.ac.wellcome.models.{Agent, Unidentifiable}

class MiroTransformableTransformerCreatorsTest
    extends FunSpec
    with MiroTransformableWrapper {
  it("if not image_creator field is present") {
    transformRecordAndCheckCreators(
      data = s""""image_title": "A guide to giraffes"""",
      expectedCreators = List()
    )
  }

  it("passes through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_creator": ["$creator"]
        """,
      expectedCreators = List(creator)
    )
  }

  it("ignores null values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Dog-owner Derek"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_creator": ["$creator1", null, "$creator2"]
        """,
      expectedCreators = List(creator1, creator2)
    )
  }

  it("passes through multiple values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Cat-wrangler Carol"
    val creator3 = "Dog-owner Derek"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_creator": ["$creator1", "$creator2", "$creator3"]
        """,
      expectedCreators = List(creator1, creator2, creator3)
    )
  }

  it("passes through a single value in the image_creator_secondary field") {
    val secondaryCreator = "Scientist Sarah"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "A radio for a racoon",
          "image_secondary_creator": ["$secondaryCreator"]
        """,
      expectedCreators = List(secondaryCreator)
    )
  }

  it("passes through multiple values in the image_creator_secondary field") {
    val secondaryCreator1 = "Gamekeeper Gordon"
    val secondaryCreator2 = "Herpetologist Harriet"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "Verdant and vivid",
          "image_secondary_creator": [
            "$secondaryCreator1", "$secondaryCreator2"
          ]
        """,
      expectedCreators = List(secondaryCreator1, secondaryCreator2)
    )
  }

  it("combines the image_creator and image_secondary_creator fields") {
    val creator = "Mycologist Morgan"
    val secondaryCreator = "Manufacturer Mel"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "Verdant and vivid",
          "image_creator": ["$creator"],
          "image_secondary_creator": ["$secondaryCreator"]
        """,
      expectedCreators = List(creator, secondaryCreator)
    )
  }

  it("passes through a value from the image_source_code field") {
    transformRecordAndCheckCreators(
      data = """
          "image_title": "A gander and a goose are game for a goof",
          "image_source_code": "GAV"
        """,
      expectedCreators = List("Isabella Gavazzi")
    )
  }

  it("does not use the image_source_code field for Wellcome Collection") {
    transformRecordAndCheckCreators(
      data = """
          "image_title": "Wandering wallabies within water",
          "image_source_code": "WEL"
        """,
      expectedCreators = List()
    )
  }

  it("does combine the image_creator and image_source_code fields") {
    val creator = "Sally Snake"
    transformRecordAndCheckCreators(
      data = s"""
          "image_title": "A gander and a goose are game for a goof",
          "image_creator": ["$creator"],
          "image_source_code": "SNL"
        """,
      expectedCreators = List(creator, "Sue Snell")
    )
  }

  private def transformRecordAndCheckCreators(
    data: String,
    expectedCreators: List[String]
  ) = {
    val transformedWork = transformWork(data = data)

    // TODO: Modify this test to use contributors when the new Miro
    // transform is done.
    // transformedWork.creators shouldBe expectedCreators.map { creator =>
    //   Unidentifiable(Agent(creator))
    // }
  }
}
