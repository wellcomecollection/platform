package uk.ac.wellcome.platform.calm_adapter.actors

import java.net.URLEncoder

import com.twitter.inject.Logging

import com.google.inject.name.Named
import javax.inject.Inject

import uk.ac.wellcome.platform.calm_adapter.modules._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.agent.Agent
import akka.actor.Actor


@Named("OaiHarvestActor")
class OaiHarvestActor @Inject()(
  actorRegister: ActorRegister,
  actorSystem: ActorSystem
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

  private def buildUri(
    path: String,
    params: Map[String, String] = Map.empty
  ): String = path + params.map {
      case (k,v) => s"${urlEncode(k)}=${urlEncode(v)}"
    }.mkString("?", "&", "")

  def receive = {
    case config: OaiHarvestActorConfig => {
      val url = buildUri(oaiUrl, config.toMap)

      info(s"Making OIA harvest request to: ${url}")

      // TODO: Should catch exceptions, maybe use different lib
      val response = Future { scala.io.Source.fromURL(url).mkString }

      response.map(r => {
        actorRegister.actors
          .get("oaiParserActor")
          .map(_ ! r)

        OaiParser.nextResumptionToken(r).map { token =>
          info(
            s"Resumption token ${token} found; scheduling new request\n" ++
            s"Next OAI request scheduled in ${RateThrottle.waitMillis}ms")

          actorSystem.scheduler.scheduleOnce(
            Duration.create(RateThrottle.waitMillis, "millis")
          )(self ! OaiHarvestActorConfig(
              verb = "ListRecords",
              token = Some(token)
            ))

        }.getOrElse(info("No <resumptionToken> in response"))
      })
    }

    case SlowDown(m) => {
      info(s"Received SlowDown message: ${m}")
      info(s"Notifying rate throttle (currently ${RateThrottle.agent.get})")

      RateThrottle.agent alter (_ + 1)
    }
    case unknown => error(s"Received unknown argument ${unknown}")
  }
}
