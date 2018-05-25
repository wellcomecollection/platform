package uk.ac.wellcome.models.work.internal

import java.io.InputStream

import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.io.Source

case class IdentifierType(
  id: String,
  label: String,
  ontologyType: String = "IdentifierType"
)

case object IdentifierType {
  private val stream: InputStream = getClass.getResourceAsStream("/identifier-schemes.csv")
  private val source = Source.fromInputStream(stream)
  private val csvRows = source
    .mkString
    .split("\n")

  // identifier-schemes.csv is a list of 3-tuples, e.g.:
  //
  //
  //    MiroImageNumber,miro-image-number,Miro image number
  //    WellcomeLibraryVideodiskNumber,wellcome-library-videodisk-number,Wellcome library videodisk number
  //    SierraSystemNumber,sierra-system-number,Sierra system number
  //
  private val identifierTypeMap: Map[String, IdentifierType] = csvRows
    .map { row =>
      val columns = row.split(",").map(_.trim)
      assert(columns.length == 3)
      Map(columns(0) -> IdentifierType(
        id = columns(1),
        label = columns(2)
      ))
    }
    .fold(Map()) { (x, y) => x ++ y }

  def apply(platformId: String): IdentifierType =
    identifierTypeMap.get(platformId) match {
      case Some(id) => id
      case None => throw GracefulFailureException(
        new RuntimeException(s"Unrecognised identifier type: $platformId")
      )
    }
}
