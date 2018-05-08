package uk.ac.wellcome.models.work.internal

case class Subject[T <: AbstractConcept](
  label: String,
  concepts: List[T],
  ontologyType: String = "Subject"
)
