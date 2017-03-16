package uk.ac.wellcome.platform.calm_adapter.actors

import java.net.URLEncoder

import akka.actor.Actor
import com.twitter.inject.Logging

import com.google.inject.name.Named
import javax.inject.Inject

import uk.ac.wellcome.platform.calm_adapter.modules._

import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent

@Named("OaiHarvestActor")
class OaiHarvestActor @Inject()(
  actorRegister: ActorRegister
)
  extends Actor
  with Logging {

  object RateThrottle {
    val agent = Agent(1)
    val throttleStep = 10L
    val throttleRoof = 1000L

    def waitMillis =
      Math.min(throttleRoof, agent.get * throttleStep)
  }

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
      val throttleMillis = RateThrottle.waitMillis
      info(s"*** Waiting for ${throttleMillis}")
      Thread.sleep(RateThrottle.waitMillis)

      var url = buildUri(oaiUrl, config.toMap)
      val response = scala.io.Source.fromURL(url).mkString

      actorRegister.actors
        .get("oaiParserActor")
        .map(_ ! response)
    }
    case SlowDown(m) => {
      info(s"Received SlowDown message: ${m}")
      info(s"Notifying rate throttle (currently ${RateThrottle.agent.get})")

      RateThrottle.agent alter (_ + 1)
    }
    case unknown => error(s"Received unknown argument ${unknown}")
  }
}
