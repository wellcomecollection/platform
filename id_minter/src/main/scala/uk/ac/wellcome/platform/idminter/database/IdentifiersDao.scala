package uk.ac.wellcome.platform.idminter.database

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.inject.Logging
import scalikejdbc._
import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.platform.idminter.model.{Identifier, IdentifiersTable}

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
  def lookupID(sourceIdentifiers: List[SourceIdentifier],
               ontologyType: String): Try[Option[Identifier]] = {

    // TODO: This exception should be handled gracefully, not sent around the
    // TryBackoff ad infinitum.
    Try {
      if (sourceIdentifiers.isEmpty) {
        throw UnableToMintIdentifierException(
          "No source identifiers supplied!")
      } else {

        blocking {
          info(
            s"About to search for existing ID matching $identifiers and $ontologyType")
          val i = identifiers.i
          val query = withSQL {
            select
              .from(identifiers as i)
              .where

              // We always want to match the ontology type, and this field
              // in SQL is never null.
              .eq(i.ontologyType, ontologyType)

              // Add conditions for matching on different source identifiers.
              .map { sql: ConditionSQLBuilder[String] =>
                addConditionForLookingUpID(
                  sql = sql,
                  sourceIdentifiers = sourceIdentifiers,
                  column = i.MiroID,
                  identifierScheme = IdentifierSchemes.miroImageNumber
                )
              }
              .map { sql: ConditionSQLBuilder[String] =>
                addConditionForLookingUpID(
                  sql = sql,
                  sourceIdentifiers = sourceIdentifiers,
                  column = i.CalmAltRefNo,
                  identifierScheme = IdentifierSchemes.calmAltRefNo
                )
              }
          }.map(Identifier(i)).single
          debug(s"Executing SQL query = '${query.statement}'")
          query.apply()
        }
      }
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
              identifiers.column.CanonicalID -> identifier.CanonicalID,
              identifiers.column.ontologyType -> identifier.ontologyType,
              identifiers.column.MiroID -> identifier.MiroID,
              identifiers.column.CalmAltRefNo -> identifier.CalmAltRefNo
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
    identifierScheme: String): ConditionSQLBuilder[String] = {
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
