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
    val workerService = injector.instance[IdMinterWorkerService]

    tableProvisioner.provision(database(), tableName())
    workerService.runSQSWorker()

    super.singletonStartup(injector)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating SQS worker")

    val system = injector.instance[ActorSystem]
    val workerService = injector.instance[IdMinterWorkerService]

    workerService.cancelRun()
    system.terminate()
  }
}
