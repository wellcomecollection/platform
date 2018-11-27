package uk.ac.wellcome.platform.merger.services

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{
  BaseWork,
  IdentifiableRedirect,
  UnidentifiedRedirectedWork,
  UnidentifiedWork
}

class MergerManagerTest extends FunSpec with Matchers with WorksGenerators {

  it("performs a merge with a single work") {
    val work = createUnidentifiedWork

    val result = mergerManager.applyMerge(maybeWorks = List(Some(work)))

    result shouldBe List(work)
  }

  it("performs a merge with multiple works") {
    val work = createUnidentifiedWork
    val otherWorks = createUnidentifiedWorks(3)

    val works = (work +: otherWorks).map { Some(_) }.toList

    val result = mergerManager.applyMerge(maybeWorks = works)

    result.head shouldBe work

    result.tail.zip(otherWorks).map {
      case (baseWork: BaseWork, unmergedWork: UnidentifiedWork) =>
        baseWork.sourceIdentifier shouldBe unmergedWork.sourceIdentifier

        val redirect = baseWork.asInstanceOf[UnidentifiedRedirectedWork]
        val redirectTarget = result.head.asInstanceOf[UnidentifiedWork]
        redirect.redirect.sourceIdentifier shouldBe redirectTarget.sourceIdentifier
    }
  }

  it("returns the works unmerged if any of the work entries are None") {
    val expectedWorks = createUnidentifiedWorks(3)

    val maybeWorks = expectedWorks.map { Some(_) } ++ List(None)

    val result = mergerManager.applyMerge(maybeWorks = maybeWorks.toList)

    result should contain theSameElementsAs expectedWorks
  }

  val mergerRules = new Merger(List()) {

    /** Make every work a redirect to the first work in the list, and leave
      * the first work intact.
      */
    override def merge(works: Seq[UnidentifiedWork]): Seq[BaseWork] =
      works.head +: works.tail.map { work =>
        UnidentifiedRedirectedWork(
          sourceIdentifier = work.sourceIdentifier,
          version = work.version,
          redirect = IdentifiableRedirect(works.head.sourceIdentifier)
        )
      }
  }

  val mergerManager = new MergerManager(mergerRules = mergerRules)
}
