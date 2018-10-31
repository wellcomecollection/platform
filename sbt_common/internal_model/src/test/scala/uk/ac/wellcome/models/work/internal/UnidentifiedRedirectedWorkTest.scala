package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators

class UnidentifiedRedirectedWorkTest
    extends FunSpec
    with Matchers
    with WorksGenerators {

  it("creates a redirect from a redirected and target work") {
    val sourceIdentifier = createMiroSourceIdentifier
    val redirectSource = createUnidentifiedWorkWith(
      sourceIdentifier = sourceIdentifier,
      version = 2)

    val targetIdentifier = createSierraSystemSourceIdentifier
    val redirectTarget = createUnidentifiedWorkWith(
      sourceIdentifier = targetIdentifier,
      version = 3)

    val redirect = UnidentifiedRedirectedWork(redirectSource, redirectTarget)

    redirect shouldBe UnidentifiedRedirectedWork(
      sourceIdentifier = sourceIdentifier,
      version = 2,
      redirect = IdentifiableRedirect(targetIdentifier)
    )
  }
}
