package uk.ac.wellcome.platform.merger.rules.singlepagemiro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.platform.merger.rules.singlepagemiro.SierraMiroMergeRule.mergeAndRedirectWork

class SierraMiroMergeRuleTest extends FunSpec with Matchers with WorksGenerators {

  it("merges a single-page miro work into a Sierra work") {
    val sierraWork =  createSierraWorkWith(items= List(createIdentifiableItemWith(locations = List(createPhysicalLocation))))
    val miroWork = createMiroWork

    println(miroWork.items)
    println(sierraWork.items)

    val result = mergeAndRedirectWork(
      Seq(
        sierraWork,
        miroWork
      ))

    val expectedMergedWork = sierraWork.copy(
      otherIdentifiers = sierraWork.otherIdentifiers ++ miroWork.identifiers,
      items = List(sierraWork.items.head
        .asInstanceOf[Identifiable[Item]]
        .copy(
          agent = sierraWork.items.head.agent.copy(
            locations = sierraWork.items.head.agent.locations ++
              miroWork.items.head.agent.locations
          )
        ))
    )

    val expectedRedirectedWork = UnidentifiedRedirectedWork(
      sourceIdentifier = miroWork.sourceIdentifier,
      version = miroWork.version,
      redirect = IdentifiableRedirect(sierraWork.sourceIdentifier)
    )

    result shouldBe List(expectedMergedWork, expectedRedirectedWork)
  }
}
