package uk.ac.wellcome.platform.calm_adapter.actors

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorSystem, PoisonPill}
import com.twitter.inject.Logging
import uk.ac.wellcome.models.CalmDynamoRecord
import uk.ac.wellcome.platform.calm_adapter.actors._
import uk.ac.wellcome.platform.finatra.modules._
import uk.ac.wellcome.platform.calm_adapter.ServerMain

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
  dynamoConfig: DynamoConfig,
  system: ActorSystem
) extends Actor
    with Logging {

  def receive = {
    case record: CalmDynamoRecord => {
      info(s"Dynamo actor received a record (${record.RecordID}).")

      ScanamoAsync.put(dynamoClient)(dynamoConfig.table)(record).map { _ =>
        info(s"Dynamo put successful (${record.RecordID}).")
      } recover {
        case e: ProvisionedThroughputExceededException => {
          error(s"Dynamo put failed (${record.RecordID})!", e)

          val message = SlowDown("Exceeded provisioned throughput!")
          actorRegister.send("oaiHarvestActor", message)

          self ! record
        }
        case x => error(s"Unknown error ${x}")
      }
    }
    case poison: PoisonPillWrapper => {
      info("Dynamo actor received a PoisonPillWrapper")
      system.scheduler.scheduleOnce(Duration.create(60, "seconds"))(
        self ! PoisonPill)
    }
    case unknown => error(s"Received unknown record object ${unknown}")
  }

  override def postStop(): Unit = {
    info("Dynamo actor finished, shutting down")
    ServerMain.close()
  }
}
