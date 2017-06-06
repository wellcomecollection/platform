package uk.ac.wellcome.platform.idminter.model

import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import scalikejdbc._

/** Represents a set of identifiers as stored in MySQL */
case class Identifier(CanonicalID: String, MiroID: String)

object Identifier {
  def apply(p: SyntaxProvider[Identifier])(rs: WrappedResultSet): Identifier =
    Identifier(rs.string(p.resultName.CanonicalID),
               rs.string(p.resultName.MiroID))
}

class IdentifiersTable @Inject()(
  @Flag("aws.rds.identifiers.database") database: String,
  @Flag("aws.rds.identifiers.table") table: String)
    extends SQLSyntaxSupport[Identifier] {
  override val schemaName = Some(database)
  override val tableName = table
  override val useSnakeCaseColumnName = false
  override val columns = Seq("MiroID", "CanonicalID")

  val i = this.syntax("i")
}
