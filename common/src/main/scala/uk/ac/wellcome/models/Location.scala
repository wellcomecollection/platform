package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

sealed trait Location{
  val locationType: String
}

case class DigitalLocation(
  url: String,
  license: License,
  locationType: String,
  credit: Option[String] = None,
  @JsonProperty("type") ontologyType: String = "DigitalLocation"
) extends Location

case class PhysicalLocation(
                           locationType: String,
                           label: String,
                           @JsonProperty("type") ontologyType: String = "PhysicalLocation"
                           ) extends Location
