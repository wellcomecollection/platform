package uk.ac.wellcome.platform.idminter.database

import com.google.inject.Inject
import com.twitter.inject.Logging
import scalikejdbc._
import uk.ac.wellcome.platform.idminter.model.{Identifier, IdentifiersTable}
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.{Future, blocking}


class IdentifiersDao @Inject()(db: DB, identifiers: IdentifiersTable) extends Logging {

  //  private val identifiersTableName = dynamoConfig.table
  implicit val session = AutoSession(db.settingsProvider)

  def findMiroIdInDb(miroId: String): Future[Option[Identifier]] =
    Future {
      blocking {
        info(s"About to search for MiroID $miroId in Identifiers")
        val i = identifiers.i
        withSQL {
          select.from(identifiers as i).where.eq(i.MiroID, miroId)
        }.map(Identifier(i)).single.apply()
      }
    } recover {
      case e: Throwable =>
        error(s"Failed getting MiroID $miroId in DynamoDB", e)
        throw e
    }

  def saveCanonicalId(identifier: Identifier): Future[Unit] = {
    val insertIntoDbFuture = Future {
      blocking {
        info(s"putting new identifier $identifier")
        withSQL {
          insert
            .into(identifiers)
            .namedValues(identifiers.column.CanonicalID -> identifier.CanonicalID,
              identifiers.column.MiroID -> identifier.MiroID)
        }.update().apply()
        ()
      }
    }
    insertIntoDbFuture.onFailure {
      case e: Exception =>
        error(
          s"Failed inserting identifier $identifier in database",
          e)
    }
    insertIntoDbFuture
  }
}
