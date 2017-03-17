package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.gu.scanamo.ScanamoFree
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.transformer.modules._
import uk.ac.wellcome.models.CalmDynamoRecord

import javax.inject.Inject
import uk.ac.wellcome.platform.transformer.modules.ActorRegistryModule
import uk.ac.wellcome.models.ActorRegister

import com.google.inject.name.Named

@Named("DynamoCaseClassExtractorActor")
class DynamoCaseClassExtractorActor @Inject()(
  actorRegister: ActorRegister
) extends Actor
    with Logging {

  def receive = {
    case record: RecordMap => {
      ScanamoFree.read[CalmDynamoRecord](record.value) match {
        case Right(o) => {
          info(s"Parsed DynamoDB record ${o}")
          actorRegister.send("transformActor", o)
        }
        case Left(o) => {
          error(s"Unable to parse record ${o}")
          // TODO: Send to dead letter queue or error
        }
      }
    }
    case event => error(s"Received unknown DynamoDB record ${event}")
  }
}
