package uk.ac.wellcome.platform.idminter.database

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.inject.Logging
import scalikejdbc._
import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}

import scala.concurrent.blocking
import scala.util.Try

case class UnableToMintIdentifierException(message: String)
    extends Exception(message)

@Singleton
class IdentifiersDao @Inject()(db: DB, identifiers: IdentifiersTable)
    extends Logging {

  implicit val session = AutoSession(db.settingsProvider)

  /* An unidentified record from the transformer can give us a list of
   * identifiers from the source systems, and an ontology type (e.g. "Work").
   *
   * This method looks for existing IDs that have matching ontology type and
   * source identifiers.
   */
  def lookupId(
    sourceIdentifier: SourceIdentifier,
    ontologyType: String
  ): Try[Option[Identifier]] = Try {

    val sourceSystem = sourceIdentifier.identifierScheme.toString
    val sourceId = sourceIdentifier.value

    // TODO: handle gracefully, don't TryBackoff ad infinitum
    blocking {
      info(s"Matching ($sourceIdentifier, $ontologyType)")

      val i = identifiers.i
      val query = withSQL {
        select
          .from(identifiers as i)
          .where
          .eq(i.OntologyType, ontologyType).and
          .eq(i.SourceSystem, sourceSystem).and
          .eq(i.SourceId, sourceId)

      }.map(Identifier(i)).single
      debug(s"Executing:'${query.statement}'")
      query.apply()
    }
  }

  /* Save an identifier into the database.
   *
   * Note that this will copy _all_ the fields on `Identifier`, nulling any
   * fields which aren't set on `Identifier`.
   */
  def saveIdentifier(identifier: Identifier): Try[Int] = {
    val insertIntoDbFuture = Try {
      blocking {
        info(s"putting new identifier $identifier")
        withSQL {
          insert
            .into(identifiers)
            .namedValues(
              identifiers.column.CanonicalId -> identifier.CanonicalId,
              identifiers.column.OntologyType -> identifier.OntologyType,
              identifiers.column.SourceSystem -> identifier.SourceSystem,
              identifiers.column.SourceId -> identifier.SourceId
            )
        }.update().apply()
      }
    }
    if (insertIntoDbFuture.isFailure) {
      insertIntoDbFuture.failed.foreach(e =>
        error(s"Failed inserting identifier $identifier in database", e))
    }
    insertIntoDbFuture
  }

  /* For a given source identifier scheme (e.g. "miro-image-number") and a
   * corresponding column in the SQL database (e.g. i.MiroID), add a condition
   * to the SQL query that looks for matching identifiers.
   *
   * Note that we look for equality _or_ null -- in the case where the
   * database only has a partial match, we want that.  Consider the following
   * example:
   *
   *            canonical ID  | Calm ID | Miro ID | Sierra ID
   *           ---------------+---------+---------+-----------
   *            zgz4vf4q      | 1234    | abcd    | null
   *
   * If we find a Sierra record with (Miro = abcd, Sierra = IVDC), we want
   * to find this record even though it doesn't have a complete match.
   */
  private def addConditionForLookingUpID(
    sql: ConditionSQLBuilder[String],
    sourceIdentifiers: List[SourceIdentifier],
    column: SQLSyntax,
    identifierScheme: IdentifierSchemes.IdentifierScheme)
    : ConditionSQLBuilder[String] = {
    val sourceID = sourceIdentifiers.filter {
      _.identifierScheme == identifierScheme
    }
    if (sourceID.isEmpty) {
      sql
    } else {
      sql.and.withRoundBracket(
        _.eq(column, sourceID.head.value).or.isNull(column))
    }
  }
}
