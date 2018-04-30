package uk.ac.wellcome.models.work.internal

case class Genre(label: String,
                 concepts: List[AbstractConcept],
                 ontologyType: String = "Genre")
