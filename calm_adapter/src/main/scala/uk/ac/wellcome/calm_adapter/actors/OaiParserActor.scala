package uk.ac.wellcome.platform.calm_adapter.actors

import javax.inject.Inject

import akka.actor.Actor
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

@Named("OaiParserActor")
class OaiParserActor @Inject()(
  actorRegister: ActorRegister
)
  extends Actor
  with Logging {

  def parseRecords(data: String): Unit = OaiParser
    .parseRecords(data)
    .map(actorRegister.send("dynamoRecordWriterActor", _))

  def receive = {
    case response: String => {
      info(s"Parser actor received some data")
      parseRecords(response)
    }
    case unknown => error(s"Received unknown OAI response ${unknown}")
  }
}
