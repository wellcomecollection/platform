package uk.ac.wellcome.platform.calm_adapter.actors

import javax.inject.Inject

import akka.actor.{Actor, PoisonPill}
import com.google.inject.name.Named
import com.twitter.inject.Logging

import uk.ac.wellcome.models.ActorRegister

import uk.ac.wellcome.platform.calm_adapter.modules._
import uk.ac.wellcome.platform.calm_adapter.services._


/** Tells the dynamoRecordWriterActor that the oaiParserActor is done.
 *
 * We don't send an Akka PoisonPill because that immediately drops any
 * messages that arrive after it on the queue.  This is just a signal not
 * to expect anything else to arrive.
 */
case class PoisonPillWrapper()


/** Actor for parsing records from the OAI-PMH responses. */
@Named("OaiParserActor")
class OaiParserActor @Inject()(
  actorRegister: ActorRegister,
  oaiParserService: OaiParserService
) extends Actor
    with Logging {

  def parseRecords(data: String): Unit =
    oaiParserService
      .parseRecords(data)
      .map(actorRegister.send("dynamoRecordWriterActor", _))

  def receive = {
    case response: String => {
      info(s"Parser actor received some data")
      parseRecords(response)
    }
    case unknown => error(s"Received unknown OAI response ${unknown}")
  }

  override def postStop(): Unit = {
    actorRegister.send("dynamoRecordWriterActor", PoisonPillWrapper())
  }
}
