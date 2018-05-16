package uk.ac.wellcome.platform.matcher

import uk.ac.wellcome.models.recorder.internal.RecorderWorkEntry
import uk.ac.wellcome.models.work.internal.{SourceIdentifier, UnidentifiedWork}

class Bah {
  def buh(workEntry: RecorderWorkEntry) =
    convertToMatchedWorks(
      RedirectFinder.redirects(convert(workEntry.work), List()))

  private def convertToMatchedWorks(
    redirects: List[Redirect]): MatchedWorksList = {
    MatchedWorksList(
      redirects
        .groupBy(_.target)
        .map {
          case (t, redirects) => MatchedWorkIds(t, redirects.map(_.source))
        }
        .toList)
  }

  private def convert(work: UnidentifiedWork): WorkUpdate = {
    WorkUpdate(
      id = buildId(work.sourceIdentifier),
      linkedIds = work.identifiers.map(buildId))
  }

  private def buildId(sourceIdentifier: SourceIdentifier): String =
    s"${sourceIdentifier.identifierScheme}/${sourceIdentifier.value}"

}
