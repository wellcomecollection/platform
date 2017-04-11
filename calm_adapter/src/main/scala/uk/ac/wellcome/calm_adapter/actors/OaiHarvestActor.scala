package uk.ac.wellcome.platform.calm_adapter.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.agent.Agent
import com.google.inject.name.Named
import com.twitter.inject.Logging
import uk.ac.wellcome.models.ActorRegister
import uk.ac.wellcome.platform.calm_adapter.services._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration.Duration

/** Configuration for the OAI harvest actor.
  *
  *  Parameters correspond to query parameters passed in the HTTP request.
  *
  *  @param verb: An OAI-PMH verb.  For a list of valid verbs, see
  *               https://www.openarchives.org/OAI/openarchivesprotocol.html#ProtocolMessages
  *  @param metadataPrefix: The metadata format to return. See
  *               http://www.openarchives.org/OAI/openarchivesprotocol.html#MetadataNamespaces
  *  @param token: The resumption token, used for fetching multiple pages of
  *               results.  See http://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl
  *
  *  Note: It is an OAI-PMH error to specify a metadata prefix and a
  *  resumption token in the same request.
  */
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

/** Trait for recording back pressure on an actor.
  *
  *  If an actor is hitting rate limits, it can apply back pressure to
  *  upstream actors to slow the rate of incoming messages.  This trait
  *  records how many times a slowdown message has been received, and how long
  *  the throttled actor should wait before processing its next message.
  */
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

/** Actor for making HTTP requests to the OAI-PMH
  *
  *  This actor does minimal parsing of the responses: it extracts the
  *  resumptionToken, but then passes the entire response body to the parser
  *  actor for full parsing.
  */
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

      // The OAI-PMH uses a resumptionToken provided in the response to
      // point to the next page of results.  If we find one, we need to make
      // another request to the OAI-PMH; if not, we're on the final page
      // of results.  For more details, see
      // http://www.openarchives.org/OAI/2.0/guidelines-harvester.htm
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
            self ! PoisonPill
          }
        }
      }
    }

    // If the DynamoRecordWriter hits write limits on DynamoDB, it asks
    // this actor to slow down fetching records from DynamoDB.  The agent
    // tracks how often we've been asked to slow down.
    //
    // This stops us from over-buffering records and running out of memory.
    case SlowDown(m) => {
      info(s"Received SlowDown message: ${m}")
      info(s"Notifying rate throttle (currently ${agent.get})")

      agent alter (_ + 1)
    }

    case unknown => error(s"Received unknown argument ${unknown}")
  }

  override def postStop(): Unit = {
    info("OAI exhausted; sending PoisonPill to oaiParserActor")
    actorRegister.send("oaiParserActor", PoisonPill)
  }
}
