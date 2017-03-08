package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.twitter.inject.Logging

import javax.inject.Inject
import com.twitter.inject.TwitterModule
import uk.ac.wellcome.platform.transformer.modules.ActorRegistryModule
import uk.ac.wellcome.platform.transformer.modules.ActorRegister
import com.google.inject.name.Named


case class RecordMap(value: java.util.Map[String, AttributeValue])

@Named("KinesisDynamoRecordExtractorActor")
class KinesisDynamoRecordExtractorActor @Inject()(
  actorRegister: ActorRegister
)
  extends Actor
  with Logging {

  def receive = {
    case record: RecordAdapter => {
      val keys = record
        .getInternalObject()
        .getDynamodb()
        .getNewImage()

      info(s"Received record ${keys}")
      actorRegister.actors
        .get("dynamoCaseClassExtractorActor")
        .map(_ ! RecordMap(keys))
    }
    case event => error(s"Received unknown Kinesis event ${event}")
  }
}
