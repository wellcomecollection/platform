package uk.ac.wellcome.models.work.internal

sealed trait Location {
  val locationType: LocationType
}

case class DigitalLocation(
  url: String,
  locationType: LocationType,
  license: Option[License] = None,
  credit: Option[String] = None,
  ontologyType: String = "DigitalLocation"
) extends Location

case class PhysicalLocation(
  locationType: LocationType,
  label: String,
  ontologyType: String = "PhysicalLocation"
) extends Location
