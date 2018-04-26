package uk.ac.wellcome.models

case class Subject(
  label: String,
  concepts: List[Concept],
  ontologyType: String = "Subject")
