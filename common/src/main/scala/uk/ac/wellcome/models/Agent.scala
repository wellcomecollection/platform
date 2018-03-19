package uk.ac.wellcome.models

sealed trait AbstractAgent {
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

case class Person(name: String,
                  prefix: Option[String] = None,
                  numeration: Option[String] = None,
                  ontologyType: String = "Person") extends AbstractAgent