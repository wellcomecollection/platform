package uk.ac.wellcome.models

sealed trait AbstractAgent {
  val label: String
}

case class Agent(
  label: String,
  identifiers: List[SourceIdentifier] = List()
) extends AbstractAgent

case class Organisation(
  label: String,
  identifiers: List[SourceIdentifier] = List()
) extends AbstractAgent

case class Person(
  label: String,
  prefixes: Option[List[String]] = None,
  numeration: Option[String] = None,
  identifiers: List[SourceIdentifier] = List()
) extends AbstractAgent
