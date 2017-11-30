package uk.ac.wellcome.platform.sierra_bib_merger.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.platform.sierra_bib_merger.services.{SierraBibMergerUpdaterService, SierraBibMergerWorkerService}
import uk.ac.wellcome.utils.TryBackoff

object SierraBibMergerModule extends TwitterModule with TryBackoff {

  override lazy val continuous: Boolean = false

  val tableName: Flag[String] = flag[String](
    name = "bibMerger.dynamo.tableName",
    help = "bibMerger dynamo table name"
  )

  @Singleton
  @Provides
  def providesSierraBibMergerUpdaterService(
     dynamoDBClient: AmazonDynamoDBAsync,
     metricsSender: MetricsSender): SierraBibMergerUpdaterService =
    new SierraBibMergerUpdaterService(
      dynamoDBClient = dynamoDBClient,
      metrics = metricsSender,
      tableName = tableName()
    )

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}package uk.ac.wellcome.platform.sierra_bib_merger.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.sierra_bib_merger.services.SierraBibMergerWorkerService

object SierraBibMergerModule extends TwitterModule {

  override def singletonStartup(injector: Injector) {
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraBibMergerWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
