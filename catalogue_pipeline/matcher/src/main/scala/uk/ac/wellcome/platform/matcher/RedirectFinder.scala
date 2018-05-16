package uk.ac.wellcome.platform.matcher

object RedirectFinder {

  def redirects(work: WorkUpdate, existingRedirects: List[Redirect]): List[Redirect] = {
    work.linkedIds match {
      case List(work.id) =>
        List(Redirect(source = work.id, target = work.id))
      case _ =>
        val maybeExistingRedirect: Option[Redirect] = existingRedirects.find(_.source == work.id)
        maybeExistingRedirect match {
          case Some(existingRedirect) => {
            val combinedIdentifier = (existingRedirect.target.split("\\+") ++ work.linkedIds).distinct.sorted.mkString("+")
            existingRedirects.filter(_.target == existingRedirect.target).map(_.copy(target = combinedIdentifier)) ++
            work.linkedIds.filterNot(_ == work.id).map(linkedId => Redirect(source=linkedId, target = combinedIdentifier)) :+
            Redirect(source = combinedIdentifier, target = combinedIdentifier)
          }
          case None => {
            val combinedIdentifier = work.linkedIds.sorted.mkString("+")
            work.linkedIds.map(li => Redirect(source = li, target = combinedIdentifier)) :+
              Redirect(source = combinedIdentifier, target = combinedIdentifier)
          }
        }
    }
  }
}

case class WorkUpdate(id :String, linkedIds: List[String])
