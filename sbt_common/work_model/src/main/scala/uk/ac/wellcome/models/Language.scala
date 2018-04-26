package uk.ac.wellcome.models

case class Language(
  id: String,
  label: String,
  ontologyType: String = "Language"
)
