package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.WorkNode
import uk.ac.wellcome.storage.GlobalExecutionContext._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

class LinkedWorkDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  val index = dynamoConfig.index.getOrElse(
    throw new RuntimeException("Index cannot be empty!"))

  def getBySetIds(setIds: Set[String]): Future[Set[WorkNode]] =
    Future.sequence(setIds.map(getBySetId)).map(_.flatten)

  def put(work: WorkNode): Future[Option[Either[DynamoReadError, WorkNode]]] = {
    Future {
      Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
    }
  }

  def get(workIds: Set[String]): Future[Set[WorkNode]] = {
    Future {
      Scanamo
        .getAll[WorkNode](dynamoDbClient)(dynamoConfig.table)('id -> workIds)
        .map {
          case Right(works) => works
          case Left(scanamoError) => {
            val exception = new RuntimeException(scanamoError.toString)
            error(
              s"An error occurred while retrieving all workIds=$workIds from DynamoDB",
              exception)
            throw exception
          }
        }
    }
  }

  private def getBySetId(setId: String) = {
    Future {
      Scanamo
        .queryIndex[WorkNode](dynamoDbClient)(dynamoConfig.table, index)(
          'componentId -> setId)
        .map {
          case Right(record) => {
            record
          }
          case Left(scanamoError) => {
            val exception = new RuntimeException(scanamoError.toString)
            error(
              s"An error occurred while retrieving bySetId=$setId from DynamoDB",
              exception
            )
            throw exception
          }
        }
    }
  }
}
