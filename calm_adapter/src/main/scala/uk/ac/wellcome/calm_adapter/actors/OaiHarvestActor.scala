package uk.ac.wellcome.platform.calm_adapter.actors

import com.twitter.inject.Logging

import com.google.inject.name.Named
import javax.inject.Inject

import uk.ac.wellcome.models.ActorRegister

import uk.ac.wellcome.platform.calm_adapter.modules._
import uk.ac.wellcome.platform.calm_adapter.services._
import uk.ac.wellcome.utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.agent.Agent
import akka.actor.Actor

case class OaiHarvestActorConfig(
  verb: String,
  metadataPrefix: Option[String] = None,
  token: Option[String] = None
) {
  def toMap =
    Map("verb" -> verb) ++
      metadataPrefix.map("metadataPrefix" -> _) ++
      token.map("resumptionToken" -> _)
}

trait Throttlable {
  val agent = Agent(1)
  val throttleStep = 10L
  val throttleRoof = 1000L
  val system: ActorSystem

  def waitMillis =
    Math.min(throttleRoof, agent.get * throttleStep)

  def throttle =
    system.scheduler.scheduleOnce(
      Duration.create(waitMillis, "millis")
    )(_)
}

@Named("OaiHarvestActor")
class OaiHarvestActor @Inject()(
  actorRegister: ActorRegister,
  actorSystem: ActorSystem,
  parserService: OaiParserService
) extends Actor
    with Logging
    with Throttlable {

  val system = actorSystem

  def receive = {
    case config: OaiHarvestActorConfig => {

      parserService.oaiHarvestRequest(config).collect {
        case ParsedOaiResult(result, resumptionToken) => {
          actorRegister.send("oaiParserActor", result)

          if (!resumptionToken.isEmpty) throttle {
            info(
              s"Resumption token ${resumptionToken} found.\n" ++
                s"Next OAI request scheduled in ${waitMillis}ms")

            self ! OaiHarvestActorConfig(verb = "ListRecords",
                                         token = resumptionToken)
          } else {
            info("No <resumptionToken> in response")
            // TODO: Termination logic goes here
          }
        }
      }
    }

    case SlowDown(m) => {
      info(s"Received SlowDown message: ${m}")
      info(s"Notifying rate throttle (currently ${agent.get})")

      agent alter (_ + 1)
    }

    case unknown => error(s"Received unknown argument ${unknown}")
  }
}
