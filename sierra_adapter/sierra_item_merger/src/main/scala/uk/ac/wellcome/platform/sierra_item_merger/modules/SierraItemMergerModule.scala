package uk.ac.wellcome.platform.sierra_item_merger.modules

import akka.actor.ActorSystem
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.services.SierraItemMergerWorkerService
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object SierraItemMergerModule extends TwitterModule {

  // eagerly load worker service
  override def singletonStartup(injector: Injector) {
    super.singletonStartup(injector)
    injector.instance[SierraItemMergerWorkerService]
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SierraObjectMerger worker")

    val workerService = injector.instance[SierraItemMergerWorkerService]
    val system = injector.instance[ActorSystem]

    workerService.stop()
    system.terminate()
  }

  @Provides
  @Singleton
  def provideItemRecordObjectStore(injector: Injector): ObjectStore[SierraItemRecord] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[SierraItemRecord]
  }
}
