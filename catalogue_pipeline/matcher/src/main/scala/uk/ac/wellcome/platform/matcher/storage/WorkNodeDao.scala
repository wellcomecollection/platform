package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.models.matcher.WorkNode
//import uk.ac.wellcome.platform.matcher.lockable._
import uk.ac.wellcome.storage.GlobalExecutionContext._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

class WorkNodeDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  val index = dynamoConfig.index

  def put(work: WorkNode): Future[Option[Either[DynamoReadError, WorkNode]]] = {
    Future {
      Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
    }
  }

  def get(ids: Set[String]): Future[Set[WorkNode]] = {
    Future {
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
  }

  def getByComponentIds(setIds: Set[String]): Future[Set[WorkNode]] =
    Future.sequence(setIds.map(getByComponentId)).map(_.flatten)

  private def getByComponentId(componentId: String) = {
    Future {
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
}
