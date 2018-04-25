package uk.ac.wellcome.models

case class Subject(
  label: String,
  concepts: List[AbstractConcept],
  ontologyType: String = "Subject")
