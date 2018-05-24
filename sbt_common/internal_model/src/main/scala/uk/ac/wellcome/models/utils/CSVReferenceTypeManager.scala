package uk.ac.wellcome.models.utils

import java.io.InputStream

import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.io.Source

class CSVReferenceTypeManager(path: String) {
  private val stream: InputStream = getClass.getResourceAsStream(path)

  // Our reference CSVs are files with three columns per row:
  //
  //    platformId,id,label
  //
  // For example:
  //
  //    2,acqi,Info Service acquisitions
  //    3,acql,Wellcome Library
  //    4,admi,administration
  //
  // The first entry is an identifier we can use to identify a row
  // within a reference file.  The second and third entries are the ID
  // and label we show in public ID schemes.
  //
  private val csvRows: Array[(String, String, String)] = Source
    .fromInputStream(stream)
    .mkString
    .split("\n")
    .map { row =>
      val columns = row.split(",").map(_.trim)
      assert(columns.length == 3)
      (columns(0), columns(1), columns(2))
    }

  private val referenceEntryMap = csvRows
    .map { case (platformId, id, label) =>
      Map(platformId -> ReferenceType(id = id, label = label))
    }
    .fold(Map()) { (x, y) => x ++ y }

  def lookupId(platformId: String) =
    referenceEntryMap.get(platformId) match {
      case Some(result) => result
      case None =>
        throw GracefulFailureException(
          new RuntimeException(s"Unrecognised identifier type: [$platformId]")
        )
    }
}
