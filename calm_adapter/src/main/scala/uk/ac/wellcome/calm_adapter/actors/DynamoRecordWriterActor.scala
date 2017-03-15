package uk.ac.wellcome.platform.calm_adapter.actors

import akka.actor.Actor
import com.twitter.inject.Logging
import uk.ac.wellcome.models.CalmDynamoRecord
import uk.ac.wellcome.platform.calm_adapter.actors._

import com.amazonaws.services.dynamodbv2._
import com.gu.scanamo._
import com.google.inject.name.Named
import javax.inject.Inject

//
// This actor receives instances of the CalmDynamoRecord case class,
// and pushes them into DynamoDB.
//


@Named("DynamoRecordWriterActor")
class DynamoRecordWriterActor @Inject()(
  dynamoClient: AmazonDynamoDB
)
  extends Actor
  with Logging {

  def receive = {
    case record: CalmDynamoRecord => {
      info(s"Dynamo actor received a record ${record}")

      val putResult = Scanamo.put(dynamoClient)("CalmData")(record)

      info(s"Dynamo put result: ${putResult}")
    }
    case unknown => error(s"Received unknown record object ${unknown}")
  }
}
