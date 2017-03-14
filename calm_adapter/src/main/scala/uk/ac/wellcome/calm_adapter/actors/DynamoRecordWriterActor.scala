package uk.ac.wellcome.platform.calm_adapter.actors

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.calm_adapter.actors._


//
// This actor receives instances of the CalmDynamoRecord case class,
// and pushes them into DynamoDB.
//
class DynamoRecordWriterActor extends Actor with Logging {

  import com.amazonaws.services.dynamodbv2._
  import com.gu.scanamo._
  def providesDynamoClient(): AmazonDynamoDB =
    AmazonDynamoDBClientBuilder
      .standard()
      .withRegion("eu-west-1")
      .build()

  val client = providesDynamoClient()

  def receive = {
    case record: CalmDynamoRecord => {
      info(s"Dynamo actor received a record ${record}")
      val putResult = Scanamo.put(client)("CalmData")(record)
      info(s"Dynamo put result: ${putResult}")

    }
    case unknown => error(s"Received unknown record object ${unknown}")
  }
}
