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
    unifiedItem.identifiers.find(identifier => identifier.sourceId == "MiroID") match {
      case Some(identifier) => Scanamo.queryIndex[Id](dynamoDBClient)("Identifiers", "MiroID")('MiroID -> identifier.value) match {
        case Right(id) :: Nil => id.CanonicalID
        case Nil =>
          val canonicalId = Identifiable.generate
          Scanamo.put(dynamoDBClient)("Identifiers")(Id(canonicalId, identifier.value))
          canonicalId
        case Right(_) :: tail => error(s"Found more than one record with MiroID ${identifier.value}")
          throw new Exception(s"Found more than one record with MiroID ${identifier.value}")
        case _ =>  error(s"Error in parsing the object with MiroID ${identifier.value}")
          throw new Exception(s"Error in parsing the object with MiroID ${identifier.value}")
      }
      case None => error(s"Item $unifiedItem did not contain a MiroID")
        throw new Exception(s"Item $unifiedItem did not contain a MiroID")
    }
  }

}
