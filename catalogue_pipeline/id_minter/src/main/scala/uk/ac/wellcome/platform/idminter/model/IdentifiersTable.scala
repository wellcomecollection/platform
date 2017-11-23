package uk.ac.wellcome.platform.idminter.model

import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
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

class IdentifiersTable @Inject()(
  @Flag("aws.rds.identifiers.database") database: String,
  @Flag("aws.rds.identifiers.table") table: String)
    extends SQLSyntaxSupport[Identifier] {
  override val schemaName = Some(database)
  override val tableName = table
  override val useSnakeCaseColumnName = false
  override val columns = Seq(
    "CanonicalID",
    "ontologyType",
    "MiroID",
    "CalmAltRefNo"
  )

  val i = this.syntax("i")
}
