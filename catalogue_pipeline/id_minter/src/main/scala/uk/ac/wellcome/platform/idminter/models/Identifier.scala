package uk.ac.wellcome.models

import scalikejdbc._

/** Represents a set of identifiers as stored in MySQL */
case class Identifier(
  ontologyType: String = "Work",
  CanonicalID: String,
  MiroID: String = null,
  CalmAltRefNo: String = null
)

object Identifier {
  def apply(p: SyntaxProvider[Identifier])(rs: WrappedResultSet): Identifier =
    Identifier(
      ontologyType = rs.string(p.resultName.ontologyType),
      CanonicalID = rs.string(p.resultName.CanonicalID),
      MiroID = rs.string(p.resultName.MiroID),
      CalmAltRefNo = rs.string(p.resultName.CalmAltRefNo)
    )
}
