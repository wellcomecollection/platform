package uk.ac.wellcome.platform.sierra_items_to_dynamo.modules

import akka.actor.ActorSystem
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_items_to_dynamo.services.SierraItemsToDynamoWorkerService
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend

import scala.concurrent.ExecutionContext

object SierraItemsToDynamoModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraItemsToDynamoWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Sierra to Dynamo worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[SierraItemsToDynamoWorkerService]

    workerService.stop()
    system.terminate()
  }

  @Provides
  @Singleton
  def provideItemRecordObjectStore(
    injector: Injector): ObjectStore[SierraItemRecord] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[SierraItemRecord]
  }
}
