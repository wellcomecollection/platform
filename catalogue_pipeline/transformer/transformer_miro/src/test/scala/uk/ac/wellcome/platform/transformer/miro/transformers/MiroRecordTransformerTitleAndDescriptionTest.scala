package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

/** Tests that the Miro transformer extracts the "title" field correctly.
  *
  *  The rules around this heuristic are somewhat fiddly, and we need to be
  *  careful that we're extracting the right fields from the Miro metadata.
  */
class MiroRecordTransformerTitleAndDescriptionTest
    extends FunSpec
    with Matchers
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("uses image_title if image_image_desc is absent") {
    val title = "A limerick about a lemming"
    transformRecordAndCheckTitle(
      miroRecord = createMiroRecordWith(
        title = Some(title)
      ),
      expectedTitle = title
    )
  }

  it("uses image_title if image_title is not a prefix of image_image_desc") {
    val title = "A tome about a turtle"
    val description = "A story of a starfish"
    transformRecordAndCheckTitle(
      miroRecord = createMiroRecordWith(
        title = Some(title),
        description = Some(description)
      ),
      expectedTitle = title,
      expectedDescription = Some(description)
    )
  }

  it(
    "uses image_image_desc if image_title is a prefix of image_image_desc (one-line description)") {
    val title = "An icon of an iguana"
    val description = "An icon of an iguana is an intriguing image"
    transformRecordAndCheckTitle(
      miroRecord = createMiroRecordWith(
        title = Some(title),
        description = Some(description)
      ),
      expectedTitle = description,
      expectedDescription = None
    )
  }

  it(
    "uses image_image_desc if image_title is a prefix of image_image_desc (multi-line description)") {
    val title = "An icon of an iguana"
    val longTitle = "An icon of an iguana is an intriguing image"
    val descriptionBody = "Woodcut, by A.R. Tist.  Italian.  1897."
    val description = s"$longTitle\n\n$descriptionBody"
    transformRecordAndCheckTitle(
      miroRecord = createMiroRecordWith(
        title = Some(title),
        description = Some(description)
      ),
      expectedTitle = longTitle,
      expectedDescription = Some(descriptionBody)
    )
  }

  describe("using the academic description if image_image_desc is unhelpful") {
    it("mixed hyphens in title/description") {
      val academicDescription = "An alibi for an academic"
      transformRecordAndCheckTitle(
        miroRecord = createMiroRecordWith(
          title = Some("-"),
          description = Some("--"),
          academicDescription = Some(academicDescription)
        ),
        expectedTitle = academicDescription
      )
    }

    it("single hyphen in title/description") {
      val academicDescription = "Using an upside-down umbrella"
      transformRecordAndCheckTitle(
        miroRecord = createMiroRecordWith(
          title = Some("-"),
          description = Some("-"),
          academicDescription = Some(academicDescription)
        ),
        expectedTitle = academicDescription
      )
    }

    it("double hyphens in title/description") {
      val academicDescription = "Dirty doubling of dastardly data"
      transformRecordAndCheckTitle(
        miroRecord = createMiroRecordWith(
          title = Some("--"),
          description = Some("--"),
          academicDescription = Some(academicDescription)
        ),
        expectedTitle = academicDescription
      )
    }

    it("multi-line academic description") {
      val academicLabel = "A lithograph of a lecturer"
      val academicBody = "The corpus of a chancellor"
      val academicDescription = s"$academicLabel\n\n$academicBody"
      transformRecordAndCheckTitle(
        miroRecord = createMiroRecordWith(
          title = Some("-"),
          description = Some("--"),
          academicDescription = Some(academicDescription)
        ),
        expectedTitle = academicLabel,
        expectedDescription = Some(academicBody)
      )
    }
  }

  it("uses image_image_desc if image_title is None") {
    val description = "A noisy narwhal in November"
    transformRecordAndCheckTitle(
      miroRecord = createMiroRecordWith(
        title = None,
        description = Some(description)
      ),
      expectedTitle = description,
      expectedDescription = None
    )
  }

  private def transformRecordAndCheckTitle(
    miroRecord: MiroRecord,
    expectedTitle: String,
    expectedDescription: Option[String] = None
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)

    transformedWork.title shouldBe expectedTitle
    transformedWork.description shouldBe expectedDescription
  }
}
