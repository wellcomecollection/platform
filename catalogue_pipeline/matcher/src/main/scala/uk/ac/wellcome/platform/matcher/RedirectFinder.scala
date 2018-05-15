package uk.ac.wellcome.platform.matcher

import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}

object RedirectFinder {

  def redirects(work: UnidentifiedWork): RedirectList = {
    work.identifiers match {
      case List(work.sourceIdentifier) =>
        RedirectList(List(Redirect(work.sourceIdentifier, List())))
      case _ =>
        val combinedIdentifier = work.identifiers.map(si => s"${si.identifierScheme}/${si.value}").sorted.mkString("+")
        RedirectList(List(Redirect(
          target = SourceIdentifier(IdentifierSchemes.mergedWork, "Work", combinedIdentifier),
          sources = work.identifiers)))
    }
  }
}
