package uk.ac.wellcome.platform.matcher.storage

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.inject.Logging
import uk.ac.wellcome.platform.matcher.models.LinkedWork
import uk.ac.wellcome.storage.GlobalExecutionContext._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.Future

class MatcherGraphDao @Inject()(
                                 dynamoDbClient: AmazonDynamoDB,
                                 dynamoConfig: DynamoConfig
                               ) extends Logging {
  def put(work: LinkedWork) = {
    Future {
      Scanamo.put(dynamoDbClient)(dynamoConfig.table)(work)
    }
  }

  def get(workId: String): Future[Option[LinkedWork]] = {
    Future {
      Scanamo.get[LinkedWork](dynamoDbClient)(dynamoConfig.table)('workId -> workId) match {
        case Some(Right(record)) => {
          Some(record)
        }
        case Some(Left(scanamoError)) =>
          val exception = new RuntimeException(scanamoError.toString)
          error(
            s"An error occurred while retrieving $workId from DynamoDB",
            exception
          )
          throw exception
        case None => {
          None
        }
      }
    }
  }
}
