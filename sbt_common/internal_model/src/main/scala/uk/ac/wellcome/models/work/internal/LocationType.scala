package uk.ac.wellcome.models.work.internal

import java.io.InputStream

import com.github.tototoshi.csv.CSVReader

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
  private val csvReader = CSVReader.open(source)
  private val csvRows = csvReader.all()

  // location-types.csv is a list of 3-tuples, e.g.:
  //
  //
  //    ThumbnailImage,thumbnail-image,Thumbnail Image
  //    2,acqi,Info Service acquisitions
  //    3,acql,Wellcome Library
  //
  private val locationTypeMap: Map[String, LocationType] = csvRows
    .map { row =>
      Map(
        row(1) -> LocationType(
          id = row(1),
          label = row(2)
        )
      )
    }
    .fold(Map()) { (x, y) =>
      x ++ y
    }

  def apply(id: String): LocationType = {
    locationTypeMap.get(id) match {
      case Some(id) => id
      case None =>
        throw new IllegalArgumentException(s"Unrecognised location type: [$id]")
    }
  }

}
