package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.models.UnifiedItem
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Id(CanonicalID: String, MiroID: String)

class IdGenerator(dynamoDBClient: AmazonDynamoDB) extends Logging {

  def generateId(unifiedItem: UnifiedItem): Future[String] = Future {
    findMiroID(unifiedItem) match {
      case Some(identifier) => findMiroIdInDynamo(identifier.value) match {
        case Right(id) :: Nil => id.CanonicalID
        case Nil => generateAndSaveCanonicalId(identifier.value)
        case Right(_) :: tail => logAndThrowError(s"Found more than one record with MiroID ${identifier.value}")
        case _ =>  logAndThrowError(s"Error in parsing the object with MiroID ${identifier.value}")
      }
      case None => logAndThrowError(s"Item $unifiedItem did not contain a MiroID")
    }
  }

  private def findMiroID(unifiedItem: UnifiedItem) =
    unifiedItem.identifiers.find(identifier => identifier.sourceId == "MiroID")

  private def findMiroIdInDynamo(miroId: String) = {
    Scanamo.queryIndex[Id](dynamoDBClient)("Identifiers", "MiroID")('MiroID -> miroId)
  }

  private def generateAndSaveCanonicalId(miroId: String) = {
    val canonicalId = Identifiable.generate
    Scanamo.put(dynamoDBClient)("Identifiers")(Id(canonicalId, miroId))
    canonicalId
  }

  private def logAndThrowError(errorMessage: String) = {
    error(errorMessage)
    throw new Exception(errorMessage)
  }
}
