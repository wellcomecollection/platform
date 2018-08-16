package uk.ac.wellcome.models.work.internal

sealed trait AbstractConcept {
  val label: String
}

sealed trait Bah extends AbstractConcept

case class Concept(label: String)
  extends Bah

case class Period(label: String)
  extends Bah

case class Place(label: String)
  extends Bah


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
                  numeration: Option[String] = None)
    extends AbstractAgent
