package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.models.work.internal.{Agent, Contributor, Unidentifiable}
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

class MiroRecordTransformerContributorsTest
    extends FunSpec
    with MiroRecordGenerators
    with MiroTransformableWrapper {
  it("if not image_creator field is present") {
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        creator = None
      ),
      expectedContributors = List()
    )
  }

  it("passes through a single value in the image_creator field") {
    val creator = "Researcher Rosie"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        creator = Some(List(Some(creator)))
      ),
      expectedContributors = List(creator)
    )
  }

  it("ignores null values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Dog-owner Derek"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        creator = Some(List(Some(creator1), None, Some(creator2)))
      ),
      expectedContributors = List(creator1, creator2)
    )
  }

  it("passes through multiple values in the image_creator field") {
    val creator1 = "Beekeeper Brian"
    val creator2 = "Cat-wrangler Carol"
    val creator3 = "Dog-owner Derek"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        creator = Some(List(Some(creator1), Some(creator2), Some(creator3)))
      ),
      expectedContributors = List(creator1, creator2, creator3)
    )
  }

  it("passes through a single value in the image_creator_secondary field") {
    val secondaryCreator = "Scientist Sarah"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        secondaryCreator = Some(List(secondaryCreator))
      ),
      expectedContributors = List(secondaryCreator)
    )
  }

  it("passes through multiple values in the image_creator_secondary field") {
    val secondaryCreator1 = "Gamekeeper Gordon"
    val secondaryCreator2 = "Herpetologist Harriet"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        secondaryCreator = Some(List(secondaryCreator1, secondaryCreator2))
      ),
      expectedContributors = List(secondaryCreator1, secondaryCreator2)
    )
  }

  it("combines the image_creator and image_secondary_creator fields") {
    val creator = "Mycologist Morgan"
    val secondaryCreator = "Manufacturer Mel"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        creator = Some(List(Some(creator))),
        secondaryCreator = Some(List(secondaryCreator))
      ),
      expectedContributors = List(creator, secondaryCreator)
    )
  }

  it("passes through a value from the image_source_code field") {
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        sourceCode = Some("GAV")
      ),
      expectedContributors = List("Isabella Gavazzi")
    )
  }

  it("does not use the image_source_code field for Wellcome Collection") {
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        sourceCode = Some("WEL")
      ),
      expectedContributors = List()
    )
  }

  it("combines the image_creator and image_source_code fields") {
    val creator = "Sally Snake"
    transformRecordAndCheckContributors(
      miroRecord = createMiroRecordWith(
        creator = Some(List(Some(creator))),
        sourceCode = Some("SNL")
      ),
      expectedContributors = List(creator, "Sue Snell")
    )
  }

  private def transformRecordAndCheckContributors(
    miroRecord: MiroRecord,
    expectedContributors: List[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    transformedWork.contributors shouldBe expectedContributors.map {
      contributor =>
        Contributor(
          agent = Unidentifiable(Agent(contributor))
        )
    }
  }
}
