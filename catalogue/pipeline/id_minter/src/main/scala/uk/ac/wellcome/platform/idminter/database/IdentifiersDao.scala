package uk.ac.wellcome.platform.idminter.database

import java.sql.SQLIntegrityConstraintViolationException

import grizzled.slf4j.Logging
import scalikejdbc._
import uk.ac.wellcome.models.work.internal.SourceIdentifier
import uk.ac.wellcome.platform.idminter.exceptions.IdMinterException
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}

import scala.concurrent.blocking
import scala.util.Try

class IdentifiersDao(db: DB, identifiers: IdentifiersTable) extends Logging {

  implicit val session = AutoSession(db.settingsProvider)

  /* An unidentified record from the transformer can give us a list of
   * identifiers from the source systems, and an ontology type (e.g. "Work").
   *
   * This method looks for existing IDs that have matching ontology type and
   * source identifiers.
   */
  def lookupId(
    sourceIdentifier: SourceIdentifier
  ): Try[Option[Identifier]] = Try {

    val sourceSystem = sourceIdentifier.identifierType.id
    val sourceId = sourceIdentifier.value

    // TODO: handle gracefully, don't TryBackoff ad infinitum
    blocking {
      debug(s"Matching ($sourceIdentifier)")

      val i = identifiers.i
      val query = withSQL {
        select
          .from(identifiers as i)
          .where
          .eq(i.OntologyType, sourceIdentifier.ontologyType)
          .and
          .eq(i.SourceSystem, sourceSystem)
          .and
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
  def saveIdentifier(identifier: Identifier): Try[Any] = {
    Try {
      blocking {
        debug(s"Putting new identifier $identifier")
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
    } recover {
      case e: SQLIntegrityConstraintViolationException =>
        warn(
          s"Unable to insert $identifier because of integrity constraints: ${e.getMessage}")
        throw IdMinterException(e)
      case e =>
        error(s"Failed inserting identifier $identifier in database", e)
        throw e
    }
  }
}
