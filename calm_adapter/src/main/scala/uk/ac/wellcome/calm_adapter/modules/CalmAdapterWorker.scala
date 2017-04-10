package uk.ac.wellcome.platform.calm_adapter.modules

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.finatra.modules.{AkkaModule, SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.models.ActorRegister
import uk.ac.wellcome.models.aws.ECSServiceScheduleRequest
import uk.ac.wellcome.platform.calm_adapter.actors._
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.utils._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object CalmAdapterWorker extends TwitterModule {

  override val modules =
    Seq(ActorRegistryModule, AkkaModule, SNSClientModule, SNSConfigModule)

  val warmupTime =
    flag(
      name = "warmupTime",
      default = 1,
      help = "Seconds to wait for dynamo scaling to occur, before starting."
    )

  override def singletonStartup(injector: Injector) {
    info("Starting Adapter worker")

    val system = injector.instance[ActorSystem]
    val actorRegister = injector.instance[ActorRegister]

    system.scheduler.scheduleOnce(
      Duration.create(warmupTime(), TimeUnit.SECONDS)
    )(
      actorRegister.send("oaiHarvestActor", oaiHarvestActorConfig)
    )
  }

  val oaiHarvestActorConfig =
    OaiHarvestActorConfig(
      verb = "ListRecords",
      metadataPrefix = Some("calm_xml")
    )

  override def singletonShutdown(injector: Injector) {
    info("Terminating Adapter worker")

    val system = injector.instance[ActorSystem]
    system.terminate()

    val snsWriter = injector.instance[SNSWriter]

    val messageBody = JsonUtil
      .toJson(
        ECSServiceScheduleRequest(
          "services_cluster",
          "calm-adapter",
          0
        )
      )

    messageBody match {
      case Success(body) => {
        snsWriter.writeMessage(body,None).map { publishRequest =>
          info(s"Sent SNS shutdown request; $publishRequest")
        }
      }
      case Failure(e) =>
        error("Failed to send ECSServiceScheduleRequest message", e)
    }
  }
}
