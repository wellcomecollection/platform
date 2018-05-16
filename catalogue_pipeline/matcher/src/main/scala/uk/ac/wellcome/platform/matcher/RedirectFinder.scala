package uk.ac.wellcome.platform.matcher

object RedirectFinder {

  def redirects(work: WorkUpdate): List[Redirect] = {
    work.linkedIds match {
      case List(work.id) =>
        List(Redirect(source = work.id, target = work.id))
      case _ =>
        val combinedIdentifier = work.linkedIds.sorted.mkString("+")
        work.linkedIds.map(li => Redirect(source = li, target = combinedIdentifier)) :+
          Redirect(source = combinedIdentifier, target = combinedIdentifier)
    }
  }
}

case class WorkUpdate(id :String, linkedIds: List[String])
