package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import scalikejdbc.{DB, select, _}
import uk.ac.wellcome.models.Identifiers.i
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Identifier, Identifiers, SourceIdentifier, Work}
import uk.ac.wellcome.platform.idminter.utils.Identifiable
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.{Future, blocking}

class IdentifierGenerator @Inject()(db: DB,
                                    dynamoDBClient: AmazonDynamoDB,
                                    dynamoConfig: DynamoConfig)
    extends Logging
    with TwitterModuleFlags {

  private val identifiersTableName = dynamoConfig.table

  def generateId(work: Work): Future[String] =
    findMiroID(work) match {
      case Some(identifier) => retrieveOrGenerateCanonicalId(identifier)
      case None =>
        error(s"Item $work did not contain a MiroID")
        Future.failed(new Exception(s"Item $work did not contain a MiroID"))
    }

  private def retrieveOrGenerateCanonicalId(
    identifier: SourceIdentifier): Future[String] =
    findMiroIdInDb(identifier.value).flatMap {
      case Some(id) => Future.successful(id.CanonicalID)
      case None => generateAndSaveCanonicalId(identifier.value)
    }

  private def findMiroID(work: Work) = {
    val maybeSourceIdentifier =
      work.identifiers.find(identifier => identifier.sourceId == "MiroID")
    info(s"SourceIdentifier: $maybeSourceIdentifier")
    maybeSourceIdentifier
  }

  private def findMiroIdInDb(miroId: String): Future[Option[Identifier]] =
    Future {
      blocking {
        implicit val session = AutoSession(db.settingsProvider)
        info(s"About to search for MiroID $miroId in $identifiersTableName")
        withSQL {
          select.from(Identifiers as i).where.eq(i.MiroID, miroId)
        }.map(Identifiers(i)).single.apply()
      }
    } recover {
      case e: Throwable =>
        error(s"Failed getting MiroID $miroId in DynamoDB", e)
        throw e
    }

  private def generateAndSaveCanonicalId(miroId: String): Future[String] = {
    val canonicalId = Identifiable.generate
    val insertIntoDbFuture = Future {
      blocking {
        implicit val session = AutoSession(db.settingsProvider)
        info(s"putting new canonicalId $canonicalId for MiroID $miroId")
        withSQL {
          insert
            .into(Identifiers)
            .namedValues(Identifiers.column.CanonicalID -> canonicalId,
                         Identifiers.column.MiroID -> miroId)
        }.update().apply()

      }
    }
    insertIntoDbFuture.onFailure {
      case e: Exception =>
        error(
          s"Failed inserting record in database for miroId: $miroId and canonicalId: $canonicalId",
          e)
    }
    insertIntoDbFuture.map { _ =>
      canonicalId
    }

  }
}
