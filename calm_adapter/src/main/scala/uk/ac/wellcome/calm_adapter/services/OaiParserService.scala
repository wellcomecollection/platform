package uk.ac.wellcome.platform.calm_adapter.services

import javax.inject.{Inject, Singleton}
import com.twitter.inject.TwitterModule
import com.twitter.inject.Logging

import java.net.URLDecoder
import scala.util.matching.Regex

import uk.ac.wellcome.models.CalmDynamoRecord
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global
import uk.ac.wellcome.platform.calm_adapter.modules.OaiHarvestConfig
import uk.ac.wellcome.platform.calm_adapter.actors.OaiHarvestActorConfig
import uk.ac.wellcome.utils.UrlUtil
import scala.concurrent.Future

case class ParsedOaiResult(
  result: String,
  resumptionToken: Option[String]
)

/** Service for parsing results from the OAI-PMH. */
@Singleton
class OaiParserService @Inject()(
  oaiHarvestConfig: OaiHarvestConfig
) extends Logging {

  /** Make an HTTP request against the OAI-PMH. */
  def oaiHarvestRequest(config: OaiHarvestActorConfig) =
    Future {
      val url = UrlUtil.buildUri(oaiHarvestConfig.oaiUrl, config.toMap)
      info(s"Making OAI harvest request to: ${url}")

      scala.io.Source.fromURL(url).mkString
    }.map(r => ParsedOaiResult(r, nextResumptionToken(r)))

  // Regex to match a resumption token, which looks something like:
  //
  //     <resumptionToken expirationDate="2017-03-09T12:00:32Z"
  //                      completeListSize="28814"
  //                      cursor="0">
  //                      b9d70a81-2154-40c8-b27c-ee8d8c3f28e7:0
  //     </resumptionToken>
  //
  val resumptionTokenPattern =
    "<resumptionToken[^>]*>([^>]+)</resumptionToken>".r("token")

  private def nextResumptionToken(data: String): Option[String] = {
    resumptionTokenPattern.findFirstMatchIn(data).map { _.group("token") }
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
  //      <fieldName urlencoded="URL%20encoded%20field%20value">
  //
  // which may have a closing slash if the value is empty.  We want to
  // capture the field name and the value.
  val streamParserPattern =
    "<([A-Za-z0-9]+) urlencoded=\"([^\"]*)\"/?>".r("name", "value")

  def parseRecords(data: String): List[CalmDynamoRecord] = {
    data
      .split("</record>")
      // After the final </record> tag, there are some closing metadata tags
      // on the XML response that we don't care about.  Discard them.
      .dropRight(1)
      .map(streamParserPattern.findAllMatchIn)
      // Although Calm records look like a key-value store, keys are not
      // necessarily unique!  So we may have to store multiple records for
      // the same key -- hence List[String].
      .map(_.foldLeft(Map[String, List[String]]())(
        (memo, element) => {
          val key = element.group("name")
          val value = URLDecoder.decode(element.group("value"), "UTF-8")

          memo ++ Map(key -> (memo.getOrElse(key, List()) ++ List(value)))
        }
      ))
      .map(
        data =>
          CalmDynamoRecord(
            data("RecordID").head,
            data("RecordType").head,
            data("AltRefNo").head,
            data("RefNo").head,
            JsonUtil.toJson(data).get
        ))
      .toList
  }
}
