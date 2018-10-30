package uk.ac.wellcome.platform.merger.fixtures

import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier
}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork

trait MatcherResultFixture {
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
        WorkIdentifier(work)
      }
}
