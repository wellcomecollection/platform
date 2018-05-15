package uk.ac.wellcome.platform.transformer.transformers

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

  it("should use the image_title field on non-V records") {
    val title = "A picture of a parrot"
    transformRecordAndCheckTitle(
      data = s""""image_title": "$title"""",
      expectedTitle = title,
      miroCollection = "Images-A"
    )
  }

  it("""
    should use the title field in the V collection if the description field
    is absent
  """) {
    val title = "A limerick about a lemming"
    transformRecordAndCheckTitle(
      data = s""""image_title": "$title"""",
      expectedTitle = title,
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the image_title field as the title on a V image if the
    image_title is not a prefix of image_image_desc
  """) {
    val title = "A tome about a turtle"
    val description = "A story of a starfish"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """,
      expectedTitle = title,
      expectedDescription = Some(description),
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the first line of image_image_desc as the title on a V image
    if image_title is a prefix of said first line, and omit a description
    entirely (one-line description)
  """) {
    val title = "An icon of an iguana"
    val description = "An icon of an iguana is an intriguing image"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """,
      expectedTitle = description,
      expectedDescription = None,
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the first line of image_image_desc as the title on a V image
    if image_title is a prefix of said first line (multi-line description)
  """) {
    val title = "An icon of an iguana"
    val longTitle = "An icon of an iguana is an intriguing image"
    val descriptionBody = "Woodcut, by A.R. Tist.  Italian.  1897."
    val description = s"$longTitle\\n\\n$descriptionBody"
    transformRecordAndCheckTitle(
      data = s"""
        "image_title": "$title",
        "image_image_desc": "$description"
      """,
      expectedTitle = longTitle,
      expectedDescription = Some(descriptionBody),
      miroCollection = "Images-V"
    )
  }

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (one-line description)
  """) {
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

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (single hyphen in the description)
  """) {
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

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (double hyphen in title and description)
  """) {
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

  it("""
    should use the image_image_desc_academic if the image_image_desc field
    doesn't contain useful data (multi-line description)
  """) {
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

  it("should use the description if image_title is None") {
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
    data: String,
    expectedTitle: String,
    expectedDescription: Option[String] = None,
    miroCollection: String = "TestCollection"
  ) = {
    val transformedWork = transformWork(
      MiroCollection = miroCollection,
      data = data
    )

    transformedWork.title shouldBe Some(expectedTitle)
    transformedWork.description shouldBe expectedDescription
  }
}
