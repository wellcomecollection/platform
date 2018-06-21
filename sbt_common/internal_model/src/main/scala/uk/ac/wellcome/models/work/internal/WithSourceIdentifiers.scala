package uk.ac.wellcome.models.work.internal

trait WithSourceIdentifiers {
  val sourceIdentifier: SourceIdentifier
  val otherIdentifiers: List[SourceIdentifier]

  def identifiers: List[SourceIdentifier] =
    List(sourceIdentifier) ++ otherIdentifiers
}
