package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}

/** Tests that the Miro transformer extracts the "title" field correctly.
  *
  *  The rules around this heuristic are somewhat fiddly, and we need to be
  *  careful that we're extracting the right fields from the Miro metadata.
  */
class MiroTransformableTransformerTitleTest
    extends FunSpec
    with Matchers
    with MiroTransformableWrapper {

  it("uses the image_title field on non-V records") {
    val title = "A picture of a parrot"
    transformRecordAndCheckTitle(
      miroId = "A0000001",
      data = s""""image_title": "$title"""",
      expectedTitle = title
    )
  }

  describe("titles on V images") {
    it("uses image_title if image_image_desc is absent") {
      val title = "A limerick about a lemming"
      transformRecordAndCheckTitle(
        miroId = "V0000001",
        data = s""""image_title": "$title"""",
        expectedTitle = title
      )
    }

    it("uses image_title if image_title is not a prefix of image_image_desc") {
      val title = "A tome about a turtle"
      val description = "A story of a starfish"
      transformRecordAndCheckTitle(
        miroId = "V0000001",
        data = s"""
          "image_title": "$title",
          "image_image_desc": "$description"
        """,
        expectedTitle = title,
        expectedDescription = Some(description)
      )
    }

    it(
      "uses image_image_desc if image_title is a prefix of image_image_desc (one-line description)") {
      val title = "An icon of an iguana"
      val description = "An icon of an iguana is an intriguing image"
      transformRecordAndCheckTitle(
        miroId = "V0000001",
        data = s"""
          "image_title": "$title",
          "image_image_desc": "$description"
        """,
        expectedTitle = description,
        expectedDescription = None
      )
    }

    it(
      "uses image_image_desc if image_title is a prefix of image_image_desc (multi-line description)") {
      val title = "An icon of an iguana"
      val longTitle = "An icon of an iguana is an intriguing image"
      val descriptionBody = "Woodcut, by A.R. Tist.  Italian.  1897."
      val description = s"$longTitle\\n\\n$descriptionBody"
      transformRecordAndCheckTitle(
        miroId = "V0000001",
        data = s"""
          "image_title": "$title",
          "image_image_desc": "$description"
        """,
        expectedTitle = longTitle,
        expectedDescription = Some(descriptionBody)
      )
    }
  }

  describe(
    "using the image_image_desc_academic if image_image_desc is unhelpful") {
    it("mixed hyphens in title/description") {
      val academicDescription = "An alibi for an academic"
      transformRecordAndCheckTitle(
        data = s"""
          "image_title": "-",
          "image_image_desc": "--",
          "image_image_desc_academic": "$academicDescription"
        """,
        expectedTitle = academicDescription
      )
    }

    it("single hyphen in title/description") {
      val academicDescription = "Using an upside-down umbrella"
      transformRecordAndCheckTitle(
        data = s"""
          "image_title": "-",
          "image_image_desc": "-",
          "image_image_desc_academic": "$academicDescription"
        """,
        expectedTitle = academicDescription
      )
    }

    it("double hyphens in title/description") {
      val academicDescription = "Dirty doubling of dastardly data"
      transformRecordAndCheckTitle(
        data = s"""
          "image_title": "--",
          "image_image_desc": "--",
          "image_image_desc_academic": "$academicDescription"
        """,
        expectedTitle = academicDescription
      )
    }

    it("multi-line academic description") {
      val academicLabel = "A lithograph of a lecturer"
      val academicBody = "The corpus of a chancellor"
      val academicDescription = s"$academicLabel\\n\\n$academicBody"
      transformRecordAndCheckTitle(
        data = s"""
          "image_title": "-",
          "image_image_desc": "--",
          "image_image_desc_academic": "$academicDescription"
        """,
        expectedTitle = academicLabel,
        expectedDescription = Some(academicBody)
      )
    }
  }

  it("uses image_image_desc if image_title is None") {
    val description = "A noisy narwhal in November"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": null,
        "image_image_desc": "$description"
      """,
      expectedTitle = description,
      expectedDescription = None
    )
  }

  private def transformRecordAndCheckTitle(
    miroId: String = "M0000001",
    data: String,
    expectedTitle: String,
    expectedDescription: Option[String] = None
  ) = {
    val transformedWork = transformWork(data = data)

    transformedWork.title shouldBe expectedTitle
    transformedWork.description shouldBe expectedDescription
  }
}
