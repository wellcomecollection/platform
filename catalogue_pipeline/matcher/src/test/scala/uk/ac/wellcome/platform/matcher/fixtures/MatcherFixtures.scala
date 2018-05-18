package uk.ac.wellcome.platform.matcher.fixtures

import uk.ac.wellcome.models.work.internal.{
  IdentifierSchemes,
  SourceIdentifier,
  UnidentifiedWork
}

trait MatcherFixtures {

  def aSierraSourceIdentifier(id: String) =
    SourceIdentifier(IdentifierSchemes.sierraSystemNumber, "Work", id)

  def anUnidentifiedSierraWork = {
    val sourceIdentifier = aSierraSourceIdentifier("id")
    UnidentifiedWork(
      sourceIdentifier = sourceIdentifier,
      title = Some("WorkTitle"),
      version = 1,
      identifiers = List(sourceIdentifier)
    )
  }
}
