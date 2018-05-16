package uk.ac.wellcome.platform.matcher

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures

class BahTest extends FunSpec with Matchers with MatcherFixtures {

  val bah = new Bah()

  it("converts a work entry with no linked identifiers to a matched works list referencing itself") {
    bah.buh(
      RecorderWorkEntry(
        "sourceId",
        "sourceName",
        anUnidentifiedSierraWork)
    ) shouldBe
    MatchedWorksList(List(MatchedWorkIds("sierra-system-number/id", List("sierra-system-number/id"))))
  }


}
