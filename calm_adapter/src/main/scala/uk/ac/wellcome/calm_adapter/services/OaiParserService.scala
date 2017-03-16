package uk.ac.wellcome.platform.calm_adapter.services

import java.net.URLDecoder
import scala.util.matching.Regex

import uk.ac.wellcome.models.CalmDynamoRecord
import uk.ac.wellcome.utils.JsonUtil


object OaiParser {

  // Regex to match a resumption token, which looks something like:
  //
  //     <resumptionToken expirationDate="2017-03-09T12:00:32Z"
  //                      completeListSize="28814"
  //                      cursor="0">
  //                      b9d70a81-2154-40c8-b27c-ee8d8c3f28e7:0
  //     </resumptionToken>
  //
  val resumptionTokenPattern = "<resumptionToken[^>]*>([^>]+)</resumptionToken>".r("token")

  def nextResumptionToken(data: String): Option[String] = {
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
  //      </record>
  //
  // or
  //
  //      <fieldName urlencoded="URL%20encoded%20field%20value">
  //
  // where the latter may have a closing slash if the value is empty.
  // In the latter case, we want to capture the field name and value.
  val streamParserPattern = "<([A-Za-z0-9]+) urlencoded=\"([^\"]*)\"/?>".r("name", "value")

  def parseRecords(data: String): List[CalmDynamoRecord] = {
    data.split("</record>")
      .dropRight(1)  // throw away the junk at the end of a response
      .map(streamParserPattern.findAllMatchIn)  // last item?
      .map(matches =>
        matches.foldLeft(Map[String, List[String]]())(
          (memo: Map[String, List[String]], element: Regex.Match) => {
            val key = element.group("name")
            val value = URLDecoder.decode(element.group("value"), "UTF-8")
            memo ++ Map[String, List[String]](
              key -> (memo.getOrElse(key, List[String]()) ++ List[String](value))
            )
          }
        )
      )
      .map(data => CalmDynamoRecord(
        data("RecordID").head,
        data("RecordType").head,
        data("AltRefNo").head,
        data("RefNo").head,
        JsonUtil.toJson(data).get
      )).toList
  }
}
