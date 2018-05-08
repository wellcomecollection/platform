package uk.ac.wellcome.models.work.internal

case class Subject[+T <: IdentityState[AbstractConcept]](
  label: String,
  concepts: List[T],
  ontologyType: String = "Subject"
)
