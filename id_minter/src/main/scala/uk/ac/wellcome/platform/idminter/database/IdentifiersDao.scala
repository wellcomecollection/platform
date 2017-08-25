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

            // This seems awfully repetitive, but I haven't been able to pin
            // down a type parameter for `sql` that lets me put this in a
            // function.
            .map { sql: ConditionSQLBuilder[String] =>
              val miroID = sourceIdentifiers.filter {
                _.identifierScheme == "miro-image-number"
              }
              if (miroID.isEmpty) {
                sql
              } else {
                sql.and.eq(i.MiroID, miroID.head.value).or.isNull(i.MiroID)
              }
            }
        }.map(Identifier(i)).single.apply()
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
