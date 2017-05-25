package uk.ac.wellcome.platform.reindexer.modules

import akka.actor.ActorSystem
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.reindexer.ServerMain
import uk.ac.wellcome.platform.reindexer.services.{CalmReindexService, MiroReindexService}
import uk.ac.wellcome.utils.TryBackoff

object ReindexModule extends TwitterModule with TryBackoff {

  val targetTableName: Flag[String] = flag[String](
    name = "reindex.target.tableName",
    help = "Reindex target table name")

  override def singletonStartup(injector: Injector) {
    info("Starting Reindexer module")

    val actorSystem = injector.instance[ActorSystem]
    val reindexService = targetTableName() match {
      case "MiroData" => injector.instance[MiroReindexService]
      case "CalmData" => injector.instance[CalmReindexService]
      case _ =>
        throw new RuntimeException(
          s"${targetTableName()} is not a recognised reindexable table.")
    }

    run(() => {
      reindexService.run
      ServerMain.close()
    }, actorSystem)

  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")
    cancelRun()

    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
