package uk.ac.wellcome.platform.transformer.actors

import javax.inject.Inject

import akka.actor.Actor
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.google.inject.name.Named
import com.twitter.inject.Logging
import uk.ac.wellcome.models.ActorRegister

case class RecordMap(value: java.util.Map[String, AttributeValue])

@Named("KinesisDynamoRecordExtractorActor")
class KinesisDynamoRecordExtractorActor @Inject()(
  actorRegister: ActorRegister
) extends Actor
    with Logging {

  def receive = {
    case record: RecordAdapter => {
      val keys = record
        .getInternalObject()
        .getDynamodb()
        .getNewImage()

      info(s"Received record ${keys}")
      actorRegister.send("dynamoCaseClassExtractorActor", RecordMap(keys))
    }
    case event => error(s"Received unknown Kinesis event ${event}")
  }
}
