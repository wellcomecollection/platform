package uk.ac.wellcome.platform.transformer.data

import java.io.InputStream

import com.github.tototoshi.csv.CSVReader
import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException

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
  // get back from the Sierra API, and the snake-case label.
  //
  private val workTypeMap: Map[Char, WorkType] = csvRows
    .map { row =>
      Map(
        Char(row(1)) -> WorkType(
          id = row(1),
          label = row(2)
        )
      )
    }
    .fold(Map()) { (x, y) =>
      x ++ y
    }

  def fromCode(code: String): WorkType = {
    code.asInstanceOf[Seq[Char]] match {
      case Seq(c) => workTypeMap.get(c) match {
        case Some(workType) => workType
        case None => throw TransformerException(
          new IllegalArgumentException(s"Unrecognised work type code: $c")
      }
      case _ => throw TransformerException(
        new IllegalArgumentException(s"Work type code is not a single character: $code")
    }
  }
}
