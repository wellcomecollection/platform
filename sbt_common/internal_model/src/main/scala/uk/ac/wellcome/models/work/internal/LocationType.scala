package uk.ac.wellcome.models.work.internal

import java.io.InputStream

import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.io.Source

case class LocationType(
  id: String,
  label: String,
  ontologyType: String = "LocationType"
)

case object LocationType {
  private val stream: InputStream =
    getClass.getResourceAsStream("/location-types.csv")
  private val source = Source.fromInputStream(stream)
  private val csvRows = source.mkString
    .split("\n")

  // location-types.csv is a list of 3-tuples, e.g.:
  //
  //
  //    ThumbnailImage,thumbnail-image,Thumbnail Image
  //    2,acqi,Info Service acquisitions
  //    3,acql,Wellcome Library
  //
  private val locationTypeMap: Map[String, LocationType] = csvRows
    .map { row =>
      val columns = row.split(",").map(_.trim)
      assert(columns.length == 3)
      Map(
        columns(1) -> LocationType(
          id = columns(1),
          label = columns(2)
        ))
    }
    .fold(Map()) { (x, y) =>
      x ++ y
    }

  def apply(id: String): LocationType =
    locationTypeMap.get(id) match {
      case Some(id) => id
      case None =>
        throw GracefulFailureException(
          new RuntimeException(s"Unrecognised location type: [$id]")
        )
    }
}
