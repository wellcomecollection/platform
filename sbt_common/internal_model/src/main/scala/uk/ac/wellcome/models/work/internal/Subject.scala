package uk.ac.wellcome.models.work.internal

case class Subject[+T <: IdentityState[AbstractRootConcept]](
  label: String,
  concepts: List[T],
  ontologyType: String = "Subject"
)
