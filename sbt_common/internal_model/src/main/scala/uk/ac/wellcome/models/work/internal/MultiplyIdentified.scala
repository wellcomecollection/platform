package uk.ac.wellcome.models.work.internal

trait MultiplyIdentified {
  val sourceIdentifier: SourceIdentifier
  val otherIdentifiers: List[SourceIdentifier]

  def identifiers: List[SourceIdentifier] =
    List(sourceIdentifier) ++ otherIdentifiers
}
