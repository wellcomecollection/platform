package uk.ac.wellcome.platform.matcher

import org.scalatest.{FunSpec, Matchers}

class RedirectFinderTest extends FunSpec with Matchers {

  it("should redirect a work without identifiers and no existing redirects") {
    val update = WorkUpdate(id = "A", linkedIds = List("A"))

    val redirectList = RedirectFinder.redirects(update, List())

    redirectList should contain theSameElementsAs List(Redirect("A", "A"))
  }

  it("should redirect a work with one link and no existing redirects") {
    val update = WorkUpdate(id = "A", linkedIds = List("A", "B"))

    val redirectList = RedirectFinder.redirects(update, List())

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B"),
      Redirect("B", "A+B"),
      Redirect("A+B", "A+B")
    )
  }

  it("should redirect a work multiple links and no existing redirects") {
    val update = WorkUpdate(id = "A", linkedIds = List("A", "B", "C"))

    val redirectList = RedirectFinder.redirects(update, List())

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B+C"),
      Redirect("B", "A+B+C"),
      Redirect("C", "A+B+C"),
      Redirect("A+B+C", "A+B+C")
    )
  }

  it(
    "should redirect a work with unordered identifiers and no existing redirects") {
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
      List(Redirect("A", "A+B"), Redirect("B", "A+B"), Redirect("A+B", "A+B")))

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B+C"),
      Redirect("B", "A+B+C"),
      Redirect("C", "A+B+C"),
      Redirect("A+B", "A+B+C"),
      Redirect("A+B+C", "A+B+C")
    )
  }

  it("A+B and C+D exist B is edited to create A+B+C+D") {
    val update = WorkUpdate(id = "B", linkedIds = List("B", "C"))

    val redirectList = RedirectFinder.redirects(
      update,
      List(
        Redirect("A", "A+B"),
        Redirect("B", "A+B"),
        Redirect("A+B", "A+B"),
        Redirect("C", "C+D"),
        Redirect("D", "C+D"),
        Redirect("C+D", "C+D")
      )
    )

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B+C+D"),
      Redirect("B", "A+B+C+D"),
      Redirect("A+B", "A+B+C+D"),
      Redirect("C", "A+B+C+D"),
      Redirect("D", "A+B+C+D"),
      Redirect("C+D", "A+B+C+D"),
      Redirect("A+B+C+D", "A+B+C+D")
    )
  }

  it("A+B exists B is edited without changing links") {
    val update = WorkUpdate(id = "B", linkedIds = List("B", "A"))

    val redirectList = RedirectFinder.redirects(
      update,
      List(Redirect("A", "A+B"), Redirect("B", "A+B"), Redirect("A+B", "A+B")))

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B"),
      Redirect("B", "A+B"),
      Redirect("A+B", "A+B"))

  }

  it("preserves existing redirects for a work without identifiers") {
    val update = WorkUpdate(id = "A", linkedIds = List("A"))

    val redirectList = RedirectFinder.redirects(
      update,
      List(Redirect("A", "A+B"), Redirect("B", "A+B"), Redirect("A+B", "A+B")))

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B"),
      Redirect("B", "A+B"),
      Redirect("A+B", "A+B")
    )
  }

  it("A+B exists B is edited to create A+B+C+D") {
    val update = WorkUpdate(id = "B", linkedIds = List("B", "C", "D"))

    val redirectList = RedirectFinder.redirects(
      update,
      List(Redirect("A", "A+B"), Redirect("B", "A+B"), Redirect("A+B", "A+B")))

    redirectList should contain theSameElementsAs List(
      Redirect("A", "A+B+C+D"),
      Redirect("B", "A+B+C+D"),
      Redirect("C", "A+B+C+D"),
      Redirect("D", "A+B+C+D"),
      Redirect("A+B", "A+B+C+D"),
      Redirect("A+B+C+D", "A+B+C+D")
    )
  }
}
