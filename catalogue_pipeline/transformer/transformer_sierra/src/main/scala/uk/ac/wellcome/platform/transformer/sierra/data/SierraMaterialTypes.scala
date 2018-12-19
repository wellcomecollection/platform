package uk.ac.wellcome.platform.transformer.sierra.data

import java.io.InputStream

import com.github.tototoshi.csv.CSVReader
import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException

import scala.io.Source

object SierraMaterialTypes {
  private val stream: InputStream =
    getClass.getResourceAsStream("/sierra-material-types.csv")
  private val source = Source.fromInputStream(stream)
  private val csvReader = CSVReader.open(source)
  private val csvRows = csvReader.all()

  // sierra-material-types.csv is a list of 4-tuples, e.g.:
  //
  //
  //    Books,a,books,Books
  //    StudentDissertations,w,student-dissertations,Student dissertations
  //
  // We care about two parts: the single letter code, which is what we
  // get back from the Sierra API, and the label.
  //
  // A couple of rows don't contain a single letter code -- we ignore them
  // for now.
  //
  private val workTypeMap: Map[Char, WorkType] = csvRows
    .map { row =>
      row(1).toList match {
        case List(char: Char) =>
          Map(
            char -> WorkType(id = row(1), label = row(3).trim)
          )
        case _ => Map[Char, WorkType]()
      }
    }
    .fold(Map()) { (x, y) =>
      x ++ y
    }

  def fromCode(code: String): WorkType = {
    code.toList match {
      case List(c) =>
        workTypeMap.get(c) match {
          case Some(workType) => workType
          case None =>
            throw SierraTransformerException(
              new IllegalArgumentException(s"Unrecognised work type code: $c"))
        }
      case _ =>
        throw SierraTransformerException(
          new IllegalArgumentException(
            s"Work type code is not a single character: <<$code>>"))
    }
  }
}
