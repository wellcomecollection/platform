package uk.ac.wellcome.platform.idminter.models

import scalikejdbc._

/** Represents a set of identifiers as stored in MySQL */
case class Identifier(
  ontologyType: String = "Work",
  CanonicalID: String,
  MiroID: String = null,
  SierraSystemNumber: String = null
)

object Identifier {
  def apply(p: SyntaxProvider[Identifier])(rs: WrappedResultSet): Identifier =
    Identifier(
      ontologyType = rs.string(p.resultName.ontologyType),
      CanonicalID = rs.string(p.resultName.CanonicalID),
      MiroID = rs.string(p.resultName.MiroID),
      SierraSystemNumber = rs.string(p.resultName.SierraSystemNumber)
    )
}
