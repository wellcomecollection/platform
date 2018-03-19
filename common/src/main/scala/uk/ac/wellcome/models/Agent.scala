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

case class Person(label: String,
                  prefixes: List[String] = Nil,
                  numeration: Option[String] = None,
                  ontologyType: String = "Person")
    extends AbstractAgent
