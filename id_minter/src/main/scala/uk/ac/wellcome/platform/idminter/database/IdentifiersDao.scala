package uk.ac.wellcome.platform.idminter.database

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.inject.Logging
import scalikejdbc._

import uk.ac.wellcome.models.SourceIdentifier
import uk.ac.wellcome.platform.idminter.model.{Identifier, IdentifiersTable}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.{Future, blocking}

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
               ontologyType: String): Future[Option[Identifier]] =
    Future {
      blocking {
        info(s"About to search for existing ID matching $identifiers and $ontologyType")
        val i = identifiers.i
        withSQL {
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
                identifierScheme = "miro-image-number"
              )
            }

            .map { sql: ConditionSQLBuilder[String] =>
              addConditionForLookingUpID(
                sql = sql,
                sourceIdentifiers = sourceIdentifiers,
                column = i.CalmAltRefNo,
                identifierScheme = "calm-altrefno"
              )
            }

        }.map(Identifier(i)).single.apply()
      }
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
  private def addConditionForLookingUpID(sql: ConditionSQLBuilder[String],
                                         sourceIdentifiers: List[SourceIdentifier],
                                         column: SQLSyntax,
                                         identifierScheme: String): ConditionSQLBuilder[String] = {
    val sourceID = sourceIdentifiers.filter { _.identifierScheme == identifierScheme }
    if (sourceID.isEmpty) {
      sql
    } else {
      sql.and.eq(column, sourceID.head.value).or.isNull(column)
    }
  }

  def lookupMiroID(miroID: String,
                   ontologyType: String = "Work"): Future[Option[Identifier]] =
    lookupID(
      sourceIdentifiers = List(
        SourceIdentifier(
          identifierScheme = "miro-image-number",
          value = miroID
        )
      ),
      ontologyType = ontologyType
    )

  def saveIdentifier(identifier: Identifier): Future[Int] = {
    val insertIntoDbFuture = Future {
      blocking {
        info(s"putting new identifier $identifier")
        withSQL {
          insert
            .into(identifiers)
            .namedValues(
              identifiers.column.CanonicalID -> identifier.CanonicalID,
              identifiers.column.ontologyType -> identifier.ontologyType,
              identifiers.column.MiroID -> identifier.MiroID
            )
        }.update().apply()
      }
    }
    insertIntoDbFuture.onFailure {
      case e: Exception =>
        error(s"Failed inserting identifier $identifier in database", e)
    }
    insertIntoDbFuture
  }
}
