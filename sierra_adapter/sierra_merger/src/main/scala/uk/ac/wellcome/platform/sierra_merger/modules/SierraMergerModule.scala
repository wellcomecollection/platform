package uk.ac.wellcome.platform.sierra_merger.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_merger.services.{
  SierraMergerUpdaterService,
  SierraMergerWorkerService
}
import uk.ac.wellcome.utils.TryBackoff

object SierraMergerModule extends TwitterModule with TryBackoff {

  flag[String]("sierra.resourceType", "", "Sierra resource type")

  override lazy val continuous: Boolean = true

  @Singleton
  @Provides
  def providesSierraMergerUpdaterService(
    dynamoDBClient: AmazonDynamoDBAsync,
    metricsSender: MetricsSender,
    dynamoConfig: DynamoConfig): SierraMergerUpdaterService =
    new SierraMergerUpdaterService(
      dynamoDBClient = dynamoDBClient,
      metrics = metricsSender,
      dynamoConfig = dynamoConfig
    )

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraMergerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraMergerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
