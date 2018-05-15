package uk.ac.wellcome.platform.matcher

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}

class RedirectFinderTest extends FunSpec with Matchers {

  it("should redirect a work without identifiers and no existing redirects") {
    val sourceIdentifier = SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", "editedWork")

    val work = unidentifiedWork.copy(
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier))

    val redirectList = RedirectFinder.redirects(work)

    redirectList shouldBe RedirectList(
      List(Redirect(
        target = sourceIdentifier,
        sources = List()))
    )
  }

  it("should redirect a work with identifiers and no existing redirects") {
      val sourceIdentifier = SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", "editedWork")
      val linkedIdentifier = SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", "linkedWork")

      val expectedCombinedIdentifier =
        SourceIdentifier(IdentifierSchemes.mergedWork, "Work", "sierra-system-number/editedWork+sierra-system-number/linkedWork")

      val work = unidentifiedWork.copy(
        sourceIdentifier = sourceIdentifier,
        identifiers = List(sourceIdentifier, linkedIdentifier))

      val redirectList = RedirectFinder.redirects(work)

      redirectList shouldBe RedirectList(
        List(Redirect(
          target = expectedCombinedIdentifier,
          sources = List(sourceIdentifier, linkedIdentifier)))
      )
    }


  private def unidentifiedWork = {
    UnidentifiedWork(
      sourceIdentifier = SourceIdentifier(
        IdentifierSchemes.sierraSystemNumber,
        "Work",
        "id"),
      title = Some("Work"),
      version = 1
    )
  }
}
