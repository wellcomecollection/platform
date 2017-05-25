package uk.ac.wellcome.platform.reindexer.modules

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.TryBackoff

object ReindexModule extends TwitterModule  with TryBackoff {

  override def singletonStartup(injector: Injector) {
    info("Starting Reindexer module")

    val actorSystem = injector.instance[ActorSystem]
    val dynamoDBClient = injector.instance[AmazonDynamoDB]

    run(() => start(), actorSystem)
  }

  private def start() = {

  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")
    cancelRun()
    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
