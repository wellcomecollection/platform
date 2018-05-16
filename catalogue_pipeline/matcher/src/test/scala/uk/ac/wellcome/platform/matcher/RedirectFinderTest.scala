package uk.ac.wellcome.platform.matcher

import org.scalatest.{FunSpec, Matchers}

class RedirectFinderTest extends FunSpec with Matchers {

  it("should redirect a work without identifiers and no existing redirects") {
    val update = WorkUpdate(id = "A", linkedIds = List("A"))

    val redirectList = RedirectFinder.redirects(update, List())

    redirectList should contain theSameElementsAs List(Redirect("A", "A"))
  }

  it("should redirect a work with identifiers and no existing redirects") {
    val update = WorkUpdate(id = "A", linkedIds = List("A", "B"))

    val redirectList = RedirectFinder.redirects(update, List())

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B"),
      Redirect("B", "A+B"),
      Redirect("A+B", "A+B")
    )
  }

  it("should redirect a work with unordered identifiers and no existing redirects") {
    val update = WorkUpdate(id = "A", linkedIds = List("B", "A"))

    val redirectList = RedirectFinder.redirects(update, List())

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B"),
      Redirect("B", "A+B"),
      Redirect("A+B", "A+B")
    )
  }

  it("A+B exists B is edited to create A+B+C") {
    val update = WorkUpdate(id = "B", linkedIds = List("B", "C"))

    val redirectList = RedirectFinder.redirects(
      update,
      List(
        Redirect("A", "A+B"),
        Redirect("B", "A+B"),
        Redirect("A+B", "A+B")))

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B+C"),
      Redirect("B", "A+B+C"),
      Redirect("C", "A+B+C"),
      Redirect("A+B", "A+B+C"),
      Redirect("A+B+C", "A+B+C")
    )
  }
  //  it("A-B exists B is edited to create A-B-C") {
  //    val sourceIdentifier = SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", "editedWork")
  //    val linkedIdentifier = SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", "linkedWork")
  //
  //    List(
  //        Redirect("A", "A-B"),
  //        Redirect("B", "A-B"),
  //        Redirect("AB", "A-B")
  //     )
//}
}
