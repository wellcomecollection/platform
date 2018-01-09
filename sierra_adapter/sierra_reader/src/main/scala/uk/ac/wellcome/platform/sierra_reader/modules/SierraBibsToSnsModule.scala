package uk.ac.wellcome.platform.sierra_reader.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_reader.services.SierraBibsToSnsWorkerService

object SierraBibsToSnsModule extends TwitterModule {
  flag[String]("sierra.apiUrl", "", "Sierra API url")
  flag[String]("sierra.oauthKey", "", "Sierra API oauth key")
  flag[String]("sierra.oauthSecret", "", "Sierra API oauth secret")
  flag[String]("sierra.fields",
               "",
               "List of fields to include in the Sierra API response")

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraBibsToSnsWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Sierra Bibs to SNS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraBibsToSnsWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
