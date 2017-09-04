package uk.ac.wellcome.models

case class UnidentifiableException()
    extends Exception("Can't find canonicalId for this Identifiable")

trait Identifiable {
  val canonicalId: Option[String]
  val identifiers: List[SourceIdentifier]
  val ontologyType: String
  def id: String = canonicalId.getOrElse(
    throw UnidentifiableException()
  )
}
