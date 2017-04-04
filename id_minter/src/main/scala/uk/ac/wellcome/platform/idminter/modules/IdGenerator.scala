package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import uk.ac.wellcome.models.UnifiedItem

import scala.concurrent.Future

class IdGenerator(dynamoDBClient: AmazonDynamoDBClient) {
  def generateId(identifiers: UnifiedItem): Future[String] = ???

}
