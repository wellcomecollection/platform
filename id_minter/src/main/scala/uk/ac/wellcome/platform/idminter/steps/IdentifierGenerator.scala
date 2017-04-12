package uk.ac.wellcome.platform.idminter.steps

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Identifier, SourceIdentifier, UnifiedItem}
import uk.ac.wellcome.platform.idminter.utils.Identifiable
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.concurrent.blocking

import scala.concurrent.Future

class IdentifierGenerator @Inject()(dynamoDBClient: AmazonDynamoDB,
                                    dynamoConfig: DynamoConfig)
    extends Logging
    with TwitterModuleFlags {

  private val identifiersTableName = dynamoConfig.table

  def generateId(unifiedItem: UnifiedItem): Future[String] =
    findMiroID(unifiedItem) match {
      case Some(identifier) => retrieveOrGenerateCanonicalId(identifier)
      case None =>
        logAndThrowError(s"Item $unifiedItem did not contain a MiroID")
    }

  private def retrieveOrGenerateCanonicalId(identifier: SourceIdentifier) =
    findMiroIdInDynamo(identifier.value).map{
      case List(Right(id)) => id.CanonicalID
      case Nil => generateAndSaveCanonicalId(identifier.value)
      case Right(_) :: tail =>
        logAndThrowError(
          s"Found more than one record with MiroID ${identifier.value}")
      case List(Left(error)) =>
        logAndThrowError(
          s"Error while reading result from Dynamo: ${error.toString}")
      case _ =>
        logAndThrowError(
          s"Error in parsing the object with MiroID ${identifier.value}")
    }


  private def findMiroID(unifiedItem: UnifiedItem) = {
    val maybeSourceIdentifier = unifiedItem.identifiers.find(identifier =>
      identifier.sourceId == "MiroID")
    info(s"SourceIdentifier: $maybeSourceIdentifier")
    maybeSourceIdentifier
  }

  private def findMiroIdInDynamo(miroId: String) = Future {
    blocking {
      info(s"About to search for MiroID $miroId in $identifiersTableName")
      Scanamo.queryIndex[Identifier](dynamoDBClient)(identifiersTableName,
        "MiroID")('MiroID -> miroId)
    }
  } recover {
    case e: Throwable =>
      error(s"Failed getting MiroId $miroId in dynamoDb", e)
      throw e
  }

  private def generateAndSaveCanonicalId(miroId: String) = {
    val canonicalId = Identifiable.generate
    blocking {
      info(s"putting new canonicalId $canonicalId for MiroID $miroId")
      Scanamo.put(dynamoDBClient)(identifiersTableName)(
        Identifier(canonicalId, miroId))
      canonicalId
    }
  }

  private def logAndThrowError(errorMessage: String) = {
    error(errorMessage)
    throw new Exception(errorMessage)
  }
}
