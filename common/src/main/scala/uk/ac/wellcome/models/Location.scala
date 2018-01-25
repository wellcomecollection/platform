package uk.ac.wellcome.models

case class Location(
  locationType: String,
  url: Option[String] = None,
  credit: Option[String] = None,
  license: License,
  ontologyType: String = "Location"
)
