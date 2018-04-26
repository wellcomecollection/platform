package uk.ac.wellcome.work_model

sealed trait Location {
  val locationType: String
}

case class DigitalLocation(
  url: String,
  license: License,
  locationType: String,
  credit: Option[String] = None,
  ontologyType: String = "DigitalLocation"
) extends Location

case class PhysicalLocation(
  locationType: String,
  label: String,
  ontologyType: String = "PhysicalLocation"
) extends Location
