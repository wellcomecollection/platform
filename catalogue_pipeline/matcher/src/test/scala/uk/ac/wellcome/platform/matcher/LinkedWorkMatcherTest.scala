package uk.ac.wellcome.platform.matcher

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{IdentifierList, LinkedWorksIdentifiersList}

class LinkedWorkMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures {

  val linkedWorkMatcher = new LinkedWorkMatcher()

  it("matches a work entry with no linked identifiers to a matched works list referencing itself") {
    linkedWorkMatcher.matchWork(anUnidentifiedSierraWork) shouldBe
      LinkedWorksIdentifiersList(
        List(IdentifierList(List("sierra-system-number/id"))))
  }

  it("matches a work entry with a linked identifier to a matched works list of identifiers") {
    val linkedIdentifier = aSierraSourceIdentifier("B")
    val aIdentifier = aSierraSourceIdentifier("A")
    val work = anUnidentifiedSierraWork.copy(
      sourceIdentifier = aIdentifier,
      identifiers = List(aIdentifier, linkedIdentifier))
    linkedWorkMatcher.matchWork(work) shouldBe
      LinkedWorksIdentifiersList(
        List(IdentifierList(List("sierra-system-number/A", "sierra-system-number/B"))))
  }

}
