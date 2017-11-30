package uk.ac.wellcome.platform.sierra_to_dynamo.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_to_dynamo.services.SierraToDynamoWorkerService

object SierraToDynamoModule extends TwitterModule {
  flag[String]("sierra.apiUrl", "", "Sierra API url")
  flag[String]("sierra.oauthKey", "", "Sierra API oauth key")
  flag[String]("sierra.oauthSecret", "", "Sierra API oauth secret")
  flag[String]("sierra.resourceType", "", "Sierra resource type")

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraToDynamoWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Sierra to Dynamo worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraToDynamoWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
