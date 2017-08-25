package uk.ac.wellcome.platform.idminter.database

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.inject.Logging
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.model.{Identifier, IdentifiersTable}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.{Future, blocking}

@Singleton
class IdentifiersDao @Inject()(db: DB, identifiers: IdentifiersTable)
    extends Logging {

  implicit val session = AutoSession(db.settingsProvider)

  def lookupMiroID(miroID: String,
                   ontologyType: String): Future[Option[Identifier]] =
    Future {
      blocking {
        info(s"About to search for MiroID $miroID in Identifiers")
        val i = identifiers.i
        withSQL {
          select
            .from(identifiers as i)
            .where
            .eq(i.ontologyType, ontologyType)
            .and
            .eq(i.MiroID, miroID)
        }.map(Identifier(i)).single.apply()
      }
    } recover {
      case e: Throwable =>
        error(s"Failed getting MiroID $miroID in Identifiers", e)
        throw e
    }

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
