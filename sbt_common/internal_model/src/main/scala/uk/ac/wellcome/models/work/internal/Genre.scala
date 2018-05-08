package uk.ac.wellcome.models.work.internal

case class Genre[T <: AbstractConcept](
  label: String,
  concepts: List[T],
  ontologyType: String = "Genre"
)
