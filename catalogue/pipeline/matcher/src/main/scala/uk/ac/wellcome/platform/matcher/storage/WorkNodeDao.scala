package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.exceptions.MatcherException
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}

class WorkNodeDao(dynamoDbClient: AmazonDynamoDB, dynamoConfig: DynamoConfig)(
  implicit ec: ExecutionContext)
    extends Logging {

  private val index = dynamoConfig.index

  def put(work: WorkNode): Future[Option[Either[DynamoReadError, WorkNode]]] =
    Future {
      Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
    }.recover {
      case exception: ProvisionedThroughputExceededException =>
        throw MatcherException(exception)
    }

  def get(ids: Set[String]): Future[Set[WorkNode]] =
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
    }.recover {
      case exception: ProvisionedThroughputExceededException =>
        throw MatcherException(exception)
    }

  def getByComponentIds(setIds: Set[String]): Future[Set[WorkNode]] =
    Future.sequence(setIds.map(getByComponentId)).map(_.flatten)

  private def getByComponentId(componentId: String) =
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
    }.recover {
      case exception: ProvisionedThroughputExceededException =>
        throw MatcherException(exception)
    }
}
