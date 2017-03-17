package uk.ac.wellcome.platform.calm_adapter.actors

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Actor
import com.twitter.inject.Logging
import uk.ac.wellcome.models.CalmDynamoRecord
import uk.ac.wellcome.platform.calm_adapter.actors._
import uk.ac.wellcome.platform.finatra.modules._

import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo._
import com.google.inject.name.Named
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import uk.ac.wellcome.models.ActorRegister


case class SlowDown(message: String)

@Named("DynamoRecordWriterActor")
class DynamoRecordWriterActor @Inject()(
  actorRegister: ActorRegister,
  dynamoClient: AmazonDynamoDBAsync,
  dynamoConfig: DynamoConfig
)
  extends Actor
  with Logging {

  def receive = {
    case record: CalmDynamoRecord => {
      info("Dynamo actor received a record.")

      ScanamoAsync.put(dynamoClient)(dynamoConfig.table)(record).map { _ =>
	info(s"Dynamo put successful.")  // todo: record ID
      } recover {
      	case e: ProvisionedThroughputExceededException => {
      	  error(s"Dynamo put failed!", e)  // todo: record ID

	  val message = SlowDown("Exceeded provisioned throughput!")
          actorRegister.send("oaiHarvestActor", message)

      	  self ! record
      	}
      }
    }
    case unknown => error(s"Received unknown record object ${unknown}")
  }
}
