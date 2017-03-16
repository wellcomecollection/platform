package uk.ac.wellcome.platform.calm_adapter.actors

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import com.twitter.inject.Logging
import uk.ac.wellcome.models.CalmDynamoRecord
import uk.ac.wellcome.platform.calm_adapter.actors._

import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo._
import com.google.inject.name.Named
import javax.inject.Inject

import uk.ac.wellcome.platform.calm_adapter.modules.ActorRegister

import scala.concurrent.ExecutionContext.Implicits.global

case class SlowDown(message: String)

@Named("DynamoRecordWriterActor")
class DynamoRecordWriterActor @Inject()(
  actorRegister: ActorRegister,
  dynamoClient: AmazonDynamoDBAsync
)
  extends Actor
  with Logging {

  def receive = {
    case record: CalmDynamoRecord => {
      info("Dynamo actor received a record.")

      ScanamoAsync.put(dynamoClient)("CalmData")(record).map { _ =>
	info(s"Dynamo put successful.")
      } recover {
  	case e: ProvisionedThroughputExceededException => {
  	  error(s"Dynamo put failed!", e)

  	  actorRegister.actors
  	    .get("OaiHarvestActor")
  	    .map(_ ! SlowDown("Exceeded provisioned throughput!"))

  	  self ! record
  	}
      }
    }
    case unknown => error(s"Received unknown record object ${unknown}")
  }
}
