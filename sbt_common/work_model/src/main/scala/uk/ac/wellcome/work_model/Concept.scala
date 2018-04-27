package uk.ac.wellcome.work_model

sealed trait AbstractConcept {
  val label: String
  val ontologyType: String
}

case class Concept(
                    label: String,
                    ontologyType: String = "Concept"
                  ) extends AbstractConcept

case class Period(
                   label: String,
                   ontologyType: String = "Period"
                 ) extends AbstractConcept

case class Place(label: String, ontologyType: String = "Place")
  extends AbstractConcept
