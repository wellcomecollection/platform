package uk.ac.wellcome.models.work.internal

sealed trait AbstractConcept {
  val label: String
}

case class Concept(label: String)
  extends AbstractConcept

case class Period(label: String)
  extends AbstractConcept

case class Place(label: String)
  extends AbstractConcept


sealed trait AbstractAgent extends AbstractConcept {
  val label: String
}

case class Agent(
  label: String
) extends AbstractAgent

case class Organisation(
  label: String
) extends AbstractAgent

case class Person(label: String,
                  prefix: Option[String] = None,
                  numeration: Option[String] = None,
                  dates: Option[String] = None)
    extends AbstractAgent
