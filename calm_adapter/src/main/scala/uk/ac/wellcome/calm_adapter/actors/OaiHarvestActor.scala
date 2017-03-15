package uk.ac.wellcome.platform.calm_adapter.actors

import java.net.URLEncoder

import akka.actor.Actor
import com.twitter.inject.Logging

import com.google.inject.name.Named
import javax.inject.Inject

import uk.ac.wellcome.platform.calm_adapter.modules._
import uk.ac.wellcome.platform.calm_adapter.modules.ActorRegister


@Named("OaiHarvestActor")
class OaiHarvestActor @Inject()(
  actorRegister: ActorRegister
)
  extends Actor
  with Logging {

  val oaiUrl = "http://archives.wellcomelibrary.org/oai/OAI.aspx"

  private def urlEncode(s: String) =
    URLEncoder.encode(s, "UTF-8")

  def buildUri(
    path: String,
    params: Map[String, String] = Map.empty
  ): String = path + params.map {
      case (k,v) => s"${urlEncode(k)}=${urlEncode(v)}"
    }.mkString("?", "&", "")

  def receive = {
    case config: OaiHarvestActorConfig => {
      var url = buildUri(oaiUrl, config.toMap)
      val response = scala.io.Source.fromURL(url).mkString

      actorRegister.actors
        .get("oaiParserActor")
        .map(_ ! response)
    }
    case unknown => error(s"Received unknown argument ${unknown}")
  }
}
