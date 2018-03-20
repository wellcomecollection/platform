package uk.ac.wellcome.models

sealed trait AbstractAgent {
  val label: String
  val ontologyType: String
}

case class Agent(
  label: String,
  ontologyType: String = "Agent"
) extends AbstractAgent

case class Organisation(
  label: String,
  ontologyType: String = "Organisation"
) extends AbstractAgent

case class IdentifiablePerson(label: String,
                  prefix: Option[String] = None,
                  numeration: Option[String] = None,
                  ontologyType: String = "Person",
                  sourceIdentifier: SourceIdentifier)
    extends AbstractAgent with Identifiable

case class UnidentifiablePerson(label: String,
                  prefix: Option[String] = None,
                  numeration: Option[String] = None,
                  ontologyType: String = "Person")
  extends AbstractAgent

