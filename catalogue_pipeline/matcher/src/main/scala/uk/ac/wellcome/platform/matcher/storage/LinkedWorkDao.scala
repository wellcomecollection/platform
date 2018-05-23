package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.LinkedWork
import uk.ac.wellcome.storage.GlobalExecutionContext._

import scala.concurrent.Future

class LinkedWorkDao @Inject()(
                                 dynamoDbClient: AmazonDynamoDB,
                                 dynamoConfig: MatcherDynamoConfig
                               ) extends Logging {
  def get(workIds: Set[String]): Future[Set[LinkedWork]] = Future.sequence(workIds.map(get)).map(_.flatten)

  def getBySetIds(setIds: Set[String]): Future[Set[LinkedWork]] = Future.sequence(setIds.map(getBySetId)).map(_.flatten)

  def put(work: LinkedWork) = {
    Future {
      Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
    }
  }

  private def get(workId: String): Future[Option[LinkedWork]] = {
    Future {
      Scanamo.get[LinkedWork](dynamoDbClient)(dynamoConfig.table)('workId -> workId) match {
        case Some(Right(record)) => {
          Some(record)
        }
        case Some(Left(scanamoError)) =>
          val exception = new RuntimeException(scanamoError.toString)
          error(
            s"An error occurred while retrieving workId=$workId from DynamoDB", exception
          )
          throw exception
        case None => {
          None
        }
      }
    }
  }

  private def getBySetId(setId: String) = {
    Future {
      Scanamo.queryIndex[LinkedWork](dynamoDbClient)(dynamoConfig.table, dynamoConfig.index)('setId -> setId)
        .map {
          case Right(record) => {
            record
          }
          case Left(scanamoError) => {
            val exception = new RuntimeException(scanamoError.toString)
            error(
              s"An error occurred while retrieving bySetId=$setId from DynamoDB", exception
            )
            throw exception
          }
        }
    }
  }

}
