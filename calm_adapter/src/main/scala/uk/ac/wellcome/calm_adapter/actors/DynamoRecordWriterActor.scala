package uk.ac.wellcome.platform.calm_adapter.actors

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.calm_adapter.actors._


//
// This actor receives instances of the CalmDynamoRecord case class,
// and pushes them into DynamoDB.
//
class DynamoRecordWriterActor extends Actor with Logging {

  def receive = {
    case record: CalmDynamoRecord => {
      info(s"Dynamo actor received a record ${record}")
    }
    case unknown => error(s"Received unknown record object ${unknown}")
  }
}
