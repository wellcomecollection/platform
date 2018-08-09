package uk.ac.wellcome.models.work.internal

import java.io.InputStream

import scala.io.Source

case class IdentifierType(
  id: String,
  label: String,
  ontologyType: String = "IdentifierType"
)

case object IdentifierType {
  private val stream: InputStream =
    getClass.getResourceAsStream("/identifier-schemes.csv")
  private val source = Source.fromInputStream(stream)
  private val csvRows = source.mkString
    .split("\n")

  // identifier-schemes.csv is a list of 2-tuples, e.g.:
  //
  //
  //    miro-image-number,Miro image number
  //    wellcome-library-videodisk-number,Wellcome library videodisk number
  //    sierra-system-number,Sierra system number
  //
  private val identifierTypeMap: Map[String, IdentifierType] = csvRows
    .map { row =>
      val columns = row.split(",").map(_.trim)
      assert(columns.length == 2)
      Map(
        columns(0) -> IdentifierType(
          id = columns(0),
          label = columns(1)
        ))
    }
    .fold(Map()) { (x, y) =>
      x ++ y
    }

  def apply(platformId: String): IdentifierType =
    identifierTypeMap.get(platformId) match {
      case Some(id) => id
      case None =>
        throw new IllegalArgumentException(
          s"Unrecognised identifier type: [$platformId]")
    }
}
