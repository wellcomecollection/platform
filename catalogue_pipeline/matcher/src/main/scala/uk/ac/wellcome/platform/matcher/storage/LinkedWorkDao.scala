package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.LinkedWork
import uk.ac.wellcome.storage.GlobalExecutionContext._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

class LinkedWorkDao @Inject()(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
) extends Logging {

  val index = dynamoConfig.index.getOrElse(
    throw new RuntimeException("Index cannot be empty!"))

  def put(
    work: LinkedWork): Future[Option[Either[DynamoReadError, LinkedWork]]] = {
    Future {
      Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
    }
  }

  // Given a collection of work IDs, return a set of all the
  // corresponding Works from the graph store.
  //
  def get(workIds: Set[String]): Future[Set[LinkedWork]] = {
    Future {
      Scanamo
        .getAll[LinkedWork](dynamoDbClient)(dynamoConfig.table)(
          'workId -> workIds)
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

  // Given a collection of Set IDs, return a set of all the works in these sets
  // from the graph store.
  //
  // Note: Each work in the table has exactly one set ID, so we can make
  // separate DB queries for each set ID without worrying about redundant calls.
  //
  def getBySetIds(setIds: Set[String]): Future[Set[LinkedWork]] =
    Future.sequence(setIds.map(getBySetId)).map(_.flatten)

  private def getBySetId(setId: String) = {
    Future {
      Scanamo
        .queryIndex[LinkedWork](dynamoDbClient)(dynamoConfig.table, index)(
          'setId -> setId)
        .map {
          case Right(record) => record
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
