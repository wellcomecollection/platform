package uk.ac.wellcome.platform.calm_adapter.actors

import java.net.URLEncoder

import akka.actor.{Actor, ActorSystem, Props}
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.calm_adapter.modules._

//
// This actor parses the "XML" response returned by the OAI-PMH harvest.
// It produces Map(String, String) instances of the key/value pairs in
// the records, and sends them for further processing.  It also starts
// subsequent OAI requests if necessary.
//
// The OAI intersperses unescaped HTML with the XML, so we can't use
// an XML parser.  Instead this class is implemented as a combination
// of regexes and stream parsers.
//
class OaiParserActor extends Actor with Logging {

  // Regex to match a resumption token, which looks something like:
  //
  //     <resumptionToken expirationDate="2017-03-09T12:00:32Z"
  //                      completeListSize="28814"
  //                      cursor="0">
  //                      b9d70a81-2154-40c8-b27c-ee8d8c3f28e7:0
  //     </resumptionToken>
  //
  val resumptionTokenPattern = "<resumptionToken[^>]*>([^>]+)</resumptionToken>".r("token")

  // An OAI response may be an incomplete list with a resumptionToken
  // value that allows us to make requests for the rest of the list.
  // Look for such a value, and if present, start a new request.
  //
  // https://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl
  def startNextRequest(response: String): Unit = {
    // TODO: Does this behave correctly when you get to the last page?
    resumptionTokenPattern.findFirstMatchIn(response) match {
      case Some(m) => {
        val token = m.group("token")
        val params = Map[String, String](
          "verb" -> "ListRecords",
          "resumptionToken" -> token
        )
        CalmAdapterWorker.oaiHarvestActor ! params
      }
      case None => info(s"No <resumptionToken> in response")
    }
  }

  def receive = {
    case response: String => {
      info(s"Parser actor received some data")
      startNextRequest(response)
    }
    case unknown => error(s"Received unknown OAI response ${unknown}")
  }
}
