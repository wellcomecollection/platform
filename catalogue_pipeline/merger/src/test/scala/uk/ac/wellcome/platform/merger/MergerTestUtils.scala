package uk.ac.wellcome.platform.merger

import org.scalatest.{Assertion, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksGenerators
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

trait MergerTestUtils
    extends Eventually
    with ScalaFutures
    with LocalVersionedHybridStore
    with WorksGenerators { this: Suite =>

  def matcherResultWith(matchedEntries: Set[Set[TransformedBaseWork]]) =
    MatcherResult(
      matchedEntries.map { works =>
        MatchedIdentifiers(worksToWorkIdentifiers(works))
      }
    )

  def worksToWorkIdentifiers(
    works: Seq[TransformedBaseWork]): Set[WorkIdentifier] =
    worksToWorkIdentifiers(works.toSet)

  def worksToWorkIdentifiers(
    works: Set[TransformedBaseWork]): Set[WorkIdentifier] =
    works
      .map { work =>
        WorkIdentifier(
          identifier = work.sourceIdentifier.toString,
          version = work.version
        )
      }

  def storeInVHS(vhs: VersionedHybridStore[TransformedBaseWork,
                                           EmptyMetadata,
                                           ObjectStore[TransformedBaseWork]],
                 work: TransformedBaseWork): Assertion = {
    vhs.updateRecord(work.sourceIdentifier.toString)(
      ifNotExisting = (work, EmptyMetadata()))((_, _) =>
      throw new RuntimeException("Not possible, VHS is empty!"))

    eventually {
      whenReady(vhs.getRecord(id = work.sourceIdentifier.toString)) { result =>
        result.get shouldBe work
      }
    }
  }

  def storeInVHS(vhs: VersionedHybridStore[TransformedBaseWork,
                                           EmptyMetadata,
                                           ObjectStore[TransformedBaseWork]],
                 entries: List[TransformedBaseWork]): List[Assertion] =
    entries.map { work =>
      storeInVHS(vhs = vhs, work = work)
    }

  def createDigitalWorkWith(
    items: List[Unidentifiable[Item]] = List(
      createUnidentifiableItemWith(locations = List(createDigitalLocation)))
  ): UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number"),
      workType = Some(WorkType("v", "E-books")),
      items = items
    )

  def createDigitalWork: UnidentifiedWork = createDigitalWorkWith()

  def createPhysicalWorkWith(
    items: List[Identifiable[Item]] = List(
      createIdentifiableItemWith(locations = List(createPhysicalLocation)))): UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number"),
      otherIdentifiers =
        List(createSourceIdentifierWith(identifierType = "sierra-identifier")),
      items = items
    )

  def createPhysicalWork: UnidentifiedWork =
    createPhysicalWorkWith()
}
