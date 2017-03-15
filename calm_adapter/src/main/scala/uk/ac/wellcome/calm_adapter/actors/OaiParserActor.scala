package uk.ac.wellcome.platform.calm_adapter.actors

import java.net.{URLDecoder, URLEncoder}
import scala.collection.mutable.{Map => MutableMap}

import akka.actor.{Actor, ActorSystem, Props}
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.calm_adapter.modules._
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.models.CalmDynamoRecord

import com.google.inject.name.Named
import javax.inject.Inject

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
        info(s"Resumption token ${token} in response; spawning new request")

        actorRegister.actors
          .get("oaiHarvestActor")
          .map(_ ! OaiHarvestActorConfig(
            verb = "ListRecords",
            token = Some(token))
          )

      }
      case None => info(s"No <resumptionToken> in response")
    }
  }

  // For annoying reasons, the OAI doesn't actually return XML.  Records
  // are returned in the form:
  //
  //      <record>
  //        <fieldName urlencoded="URL%20encoded%20field%20value">
  //          URL encoded field value
  //        </fieldName>
  //        ...
  //      </record>
  //
  // where the field value may contain unescaped HTML characters.  This
  // breaks most XML parsers, which attempt to parse the HTML.
  //
  // Rather than trying to distinguish the HTML from XML, we simply pick
  // out those `urlencoded` values, and feed those into our map.  We stream
  // over the entire response string, picking out those `urlencoded` fields
  // for key/value pairs, and look for a `</record>` tag to denote the
  // end of the record.

  // This regex has to match expressions of the form
  //
  //      </record>
  //
  // or
  //
  //      <fieldName urlencoded="URL%20encoded%20field%20value">
  //
  // where the latter may have a closing slash if the value is empty.
  // In the latter case, we want to capture the field name and value.
  val streamParserPattern = "<(?:/record|([A-Za-z0-9]+) urlencoded=\"([^\"]*)\"/?)>".r("name", "value")

  def parseRecords(data: String): Unit = {
    // Stores the name/value pairs found so far in each record
    var currentRecord: MutableMap[String, String] = MutableMap()

    for (m <- streamParserPattern.findAllMatchIn(data)) {
      if (m.matched == "</record>") {
        // We're at the end of a record -- wrap up any fields we've found in a
        // CalmDynamoRecord class and send it for further processing.
        // TODO: Error handling
        val dynamoRecord = CalmDynamoRecord(
          currentRecord("RecordID"),
          currentRecord("RecordType"),
          currentRecord("AltRefNo"),
          currentRecord("RefNo"),
          JsonUtil.toJson(currentRecord).get
        )

        actorRegister.actors
          .get("dynamoRecordWriterActor")
          .map(_ ! dynamoRecord)

        currentRecord = MutableMap()
      } else {
        currentRecord.update(
          m.group("name"),
          URLDecoder.decode(m.group("value"), "UTF-8"))
      }
    }
  }

  def receive = {
    case response: String => {
      info(s"Parser actor received some data")
      startNextRequest(response)
      parseRecords(response)
    }
    case unknown => error(s"Received unknown OAI response ${unknown}")
  }
}
