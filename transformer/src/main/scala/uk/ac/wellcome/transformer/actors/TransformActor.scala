package uk.ac.wellcome.platform.transformer.actors

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.models._
import scala.util.Success
import scala.util.Failure

import javax.inject.Inject
import uk.ac.wellcome.platform.transformer.modules.ActorRegistryModule
import uk.ac.wellcome.platform.transformer.modules.ActorRegister
import com.google.inject.name.Named


@Named("TransformActor")
class TransformActor @Inject()(
  actorRegister: ActorRegister
)
  extends Actor
  with Logging {

  def receive = {
    case dirtyRecord: Transformable => {
      dirtyRecord.transform match {
        case Success(cleanRecord) => {
          info(s"Cleaned record ${cleanRecord}")

          actorRegister.actors
	    .get("publishableMessageRecordActor")
	    .map(_ ! cleanRecord)
        }
        case Failure(e) => {
          // Send to dead letter queue or just error
          error("Failed to perform transform to clean record", e)
        }
      }
    }
    case record => error(s"Received non-transformable record ${record}")
  }
}
