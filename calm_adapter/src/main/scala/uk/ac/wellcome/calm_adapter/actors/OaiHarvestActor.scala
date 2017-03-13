package uk.ac.wellcome.platform.calm_adapter.actors

import java.net.URLEncoder

import akka.actor.Actor
import com.twitter.inject.Logging

import uk.ac.wellcome.platform.calm_adapter.modules._

//
// This actor makes requests against the OAI, then spits out the XML bodies
// and sends them to another actor for further processing.
//
class OaiHarvestActor extends Actor with Logging {

  // URL to our OAI installation
  val oaiUrl = "http://archives.wellcomelibrary.org/oai/OAI.aspx"

  val defaultParams: Map[String, String] = Map(
    // https://www.openarchives.org/OAI/openarchivesprotocol.html#ListRecords
    "verb" -> "ListRecords",

    // This format is defined in our OAI installation
    "metadataPrefix" -> "calm_xml"
  )

  // Utility method.  Given a URL and some query parameters, construct
  // the full URL.
  def buildUri(path: String, params: Map[String, String] = Map.empty): String = {
    if (params.isEmpty) {
      path
    } else {
      val param_str = params.map {
        case (k,v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("?", "&", "")
      path + param_str
    }
  }

  def receive = {
    // TODO: Understand and fix the type warning that's coming from here
    case params: Map[String, String] => {
      val url = buildUri(oaiUrl, defaultParams ++ params)

      // TODO: We have Finatra and Finagle available.  Use those instead?
      // TODO: Error handling.
      val response = scala.io.Source.fromURL(url).mkString

      CalmAdapterWorker.oaiParserActor ! response
    }
    case unknown => error(s"Received unknown argument ${unknown}")
  }
}
