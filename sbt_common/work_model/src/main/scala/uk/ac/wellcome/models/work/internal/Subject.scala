package uk.ac.wellcome.models.work.internal

case class Subject(label: String,
                   concepts: List[AbstractConcept],
                   ontologyType: String = "Subject")
