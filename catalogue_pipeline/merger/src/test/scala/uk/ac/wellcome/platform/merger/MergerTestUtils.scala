package uk.ac.wellcome.platform.merger

import org.scalatest.{Assertion, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier}
import uk.ac.wellcome.models.work.internal.{TransformedBaseWork, UnidentifiedWork, WorkType}
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

trait MergerTestUtils
    extends Eventually
    with ScalaFutures
    with LocalVersionedHybridStore
    with WorksUtil { this: Suite =>

  def matcherResultWith(matchedEntries: Set[Set[TransformedBaseWork]]) =
    MatcherResult(
      matchedEntries.map {
        works => MatchedIdentifiers(worksToWorkIdentifiers(works))
      }
    )

  def worksToWorkIdentifiers(
    works: Seq[TransformedBaseWork]): Set[WorkIdentifier] =
    worksToWorkIdentifiers(works.toSet)

  def worksToWorkIdentifiers(
    works: Set[TransformedBaseWork]): Set[WorkIdentifier] = works
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
      whenReady(vhs.getRecord(id = id(work))) { result =>
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

  def createDigitalWork: UnidentifiedWork = {
    createUnidentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "miro-image-number"),
      otherIdentifiers = List(
        createSourceIdentifierWith(identifierType = "miro-library-reference")),
      workType = Some(WorkType("v", "E-books")),
      items = List(
        createUnidentifiableItemWith(locations = List(createDigitalLocation)))
    )
  }

  def createPhysicalWork: UnidentifiedWork = {
    createUnidentifiedWorkWith(
      sourceIdentifier =
        createSourceIdentifierWith(identifierType = "sierra-system-number"),
      otherIdentifiers =
        List(createSourceIdentifierWith(identifierType = "sierra-identifier")),
      items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation)))
    )
  }
}
