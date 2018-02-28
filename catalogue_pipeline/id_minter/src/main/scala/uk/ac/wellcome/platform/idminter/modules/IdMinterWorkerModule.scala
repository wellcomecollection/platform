package uk.ac.wellcome.platform.idminter.modules

import akka.actor.ActorSystem
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.idminter.database.TableProvisioner
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService

object IdMinterWorkerModule extends TwitterModule {
  val database = flag[String](
    "aws.rds.identifiers.database",
    "",
    "Name of the identifiers database")
  val tableName = flag[String](
    "aws.rds.identifiers.table",
    "",
    "Name of the identifiers table")

  override def singletonStartup(injector: Injector) {
    val tableProvisioner = injector.instance[TableProvisioner]

    tableProvisioner.provision(database(), tableName())

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
