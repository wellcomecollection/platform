package uk.ac.wellcome.platform.matcher

object RedirectFinder {

  def redirects(work: WorkUpdate, existingRedirects: List[Redirect]): List[Redirect] = {
    val directlyAffectedExistingRedirects = existingRedirects.filter(redirect => work.linkedIds.contains(redirect.source))

    val matchedWorkId = combinedIdentifier(work.linkedIds, directlyAffectedExistingRedirects)

    (redirectToCombined(existingRedirects, matchedWorkId) ++
      newRedirects(work.linkedIds, matchedWorkId) :+
      selfRedirect(matchedWorkId)).distinct
  }

  private def selfRedirect(combinedIdentifier: String) = {
    Redirect(source = combinedIdentifier, target = combinedIdentifier)
  }

  private def newRedirects(linkedIds: List[String], combinedIdentifier: String) = {
    linkedIds.map(linkedId => Redirect(source = linkedId, target = combinedIdentifier))
  }

  private def redirectToCombined(existingRedirects: List[Redirect], combinedIdentifier: String) = {
    existingRedirects.map(_.copy(target = combinedIdentifier))
  }

  private def combinedIdentifier(linkedIds: List[String], directlyAffectedRedirects: List[Redirect]) = {
    (getParticipatingNodeIds(directlyAffectedRedirects) ++ linkedIds).distinct.sorted.mkString("+")
  }

  private def getParticipatingNodeIds(directlyAffectedRedirects: List[Redirect]) = {
    directlyAffectedRedirects.flatMap(_.target.split("\\+"))
  }
}

case class WorkUpdate(id :String, linkedIds: List[String])
