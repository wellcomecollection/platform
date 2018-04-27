package uk.ac.wellcome.models.work.internal

sealed trait AbstractAgent {
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
