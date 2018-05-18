package uk.ac.wellcome.platform.matcher

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{IdentifierList, LinkedWorksList}

class LinkedWorkMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures {

  val linkedWorkMatcher = new LinkedWorkMatcher()

  it("converts a work entry with no linked identifiers to a matched works list referencing itself") {
    linkedWorkMatcher.matchWork(anUnidentifiedSierraWork) shouldBe
      LinkedWorksList(
        List(IdentifierList(List("sierra-system-number/id"))))
  }

}
