package uk.ac.wellcome.platform.matcher

import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}

class LinkedWorkMatcher {
  def matchWork(workEntry: RecorderWorkEntry) =
    asMatchedWorks(
      RedirectFinder.redirects(extractIdentifiers(workEntry.work), List()))

  private def asMatchedWorks(redirects: List[Redirect]): MatchedWorksList = {
    MatchedWorksList(
      redirects
        .groupBy(_.target)
        .map {
          case (t, redirects) => MatchedWorkIds(t, redirects.map(_.source))
        }
        .toList)
  }

  private def extractIdentifiers(work: UnidentifiedWork): WorkUpdate = {
    WorkUpdate(
      id = identifierToString(work.sourceIdentifier),
      linkedIds = work.identifiers.map(identifierToString))
  }

  private def identifierToString(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierScheme}/${sourceIdentifier.value}"
}
