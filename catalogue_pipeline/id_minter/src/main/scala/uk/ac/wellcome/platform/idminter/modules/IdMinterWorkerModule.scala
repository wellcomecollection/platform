package uk.ac.wellcome.platform.idminter.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.idminter.config.models.IdentifiersTableConfig
import uk.ac.wellcome.platform.idminter.database.TableProvisioner
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService

object IdMinterWorkerModule extends TwitterModule {
  override def singletonStartup(injector: Injector) {
    val tableProvisioner = injector.instance[TableProvisioner]
    val identifiersTableConfig = injector.instance[IdentifiersTableConfig]

    tableProvisioner.provision(
      database = identifiersTableConfig.database,
      tableName = identifiersTableConfig.tableName
    )

    injector.instance[IdMinterWorkerService]
    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[IdMinterWorkerService]

    workerService.stop()
    system.terminate()
  }
}
