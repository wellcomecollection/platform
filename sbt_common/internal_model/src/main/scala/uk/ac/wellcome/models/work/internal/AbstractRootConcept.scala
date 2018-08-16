package uk.ac.wellcome.models.work.internal


sealed trait AbstractRootConcept {
  val label: String
}

sealed trait AbstractConcept extends AbstractRootConcept

case class Concept(label: String)
  extends AbstractConcept

case class Period(label: String)
  extends AbstractConcept

case class Place(label: String)
  extends AbstractConcept


sealed trait AbstractAgent extends AbstractRootConcept

case class Agent(
  label: String
) extends AbstractAgent

case class Organisation(
  label: String
) extends AbstractAgent

case class Person(label: String,
                  prefix: Option[String] = None,
                  numeration: Option[String] = None)
    extends AbstractAgent
