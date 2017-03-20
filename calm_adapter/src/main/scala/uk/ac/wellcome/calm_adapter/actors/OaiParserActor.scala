package uk.ac.wellcome.platform.calm_adapter.actors

import javax.inject.Inject

import akka.actor.{Actor, PoisonPill}
import com.google.inject.name.Named
import com.twitter.inject.Logging

import uk.ac.wellcome.models.ActorRegister

import uk.ac.wellcome.platform.calm_adapter.modules._
import uk.ac.wellcome.platform.calm_adapter.services._

// This actor parses the "XML" response returned by the OAI-PMH harvest.
// It produces Map(String, String) instances of the key/value pairs in
// the records, and sends them for further processing.  It also starts
// subsequent OAI requests if necessary.
//
// The OAI intersperses unescaped HTML with the XML, so we can't use
// an XML parser.  Instead this class is implemented as a combination
// of regexes and stream parsers.

/**
 * Tells the dynamoRecordWriterActor that the oaiParserActor is done.
 *
 * We don't send an Akka PoisonPill because that immediately drops any
 * messages that arrive after it on the queue.  This is just a signal not
 * to expect anything else to arrive.
 */
case class PoisonPillWrapper()


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
