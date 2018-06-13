package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.storage.dynamo.DynamoConfig

class WorkNodeDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  val index = dynamoConfig.index

  def put(work: WorkNode): Option[Either[DynamoReadError, WorkNode]] = {
    Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
  }

  def get(ids: Set[String]): Set[WorkNode] = {
    Scanamo
      .getAll[WorkNode](dynamoDbClient)(dynamoConfig.table)('id -> ids)
      .map {
        case Right(works) => works
        case Left(scanamoError) => {
          val exception = new RuntimeException(scanamoError.toString)
          error(
            s"An error occurred while retrieving all workIds=$ids from DynamoDB",
            exception)
          throw exception
        }
      }
  }

  def getByComponentIds(setIds: Set[String]): Set[WorkNode] =
    setIds.flatMap(getByComponentId)

  private def getByComponentId(componentId: String) = {
      Scanamo
        .queryIndex[WorkNode](dynamoDbClient)(dynamoConfig.table, index)(
          'componentId -> componentId)
        .map {
          case Right(record) => {
            record
          }
          case Left(scanamoError) => {
            val exception = new RuntimeException(scanamoError.toString)
            error(
              s"An error occurred while retrieving byComponentId=$componentId from DynamoDB",
              exception
            )
            throw exception
          }
        }
  }
}
