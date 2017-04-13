package uk.ac.wellcome.platform.calm_adapter.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem, PoisonPill}
import com.amazonaws.services.dynamodbv2._
import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.name.Named
import com.gu.scanamo._
import com.twitter.inject.Logging
import uk.ac.wellcome.models.{ActorRegister, CalmTransformable}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.calm_adapter.ServerMain
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration

/** Used to tell the OaiHarvestActor to slow down.
  *
  *  This is sent when we hit write limits in DynamoDB, and we want to avoid
  *  over-buffering records within the application.
  */
case class SlowDown(message: String)

/** Actor for writing records to DynamoDB. */
@Named("DynamoRecordWriterActor")
class DynamoRecordWriterActor @Inject()(
  actorRegister: ActorRegister,
  dynamoClient: AmazonDynamoDBAsync,
  dynamoConfig: DynamoConfig,
  system: ActorSystem
) extends Actor
    with Logging {

  def receive = {
    case record: CalmTransformable => {
      info(s"Dynamo actor received a record (${record.RecordID}).")

      // We try to write a record, but if we hit write limits, we don't
      // want to lose the record.  Instead, we:
      //
      //  * Send a SlowDown message to the harvest actor, to slow the flow
      //    of incoming results from the OAI-PMH.
      //  * Send the record back around for a second run.
      //
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

      // When all the records from the OAI-PMH have been harvested and parsed,
      // we give this actor another 60 seconds to shutdown: giving it time to
      // flush out any remaining records.
      //
      // A PoisonPill allows any records already on the queue to be processed
      // out, but records that arrive after the PoisonPill are lost.  Because
      // we push records back on to the queue if they hit write limits, we
      // don't want the upstream actor to send a PoisonPill directly, or we
      // might lose a significant number of in-flight records.
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
