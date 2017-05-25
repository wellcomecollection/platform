package uk.ac.wellcome.platform.reindexer.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.{CalmTransformable, MiroTransformable}
import uk.ac.wellcome.platform.reindexer.ServerMain
import uk.ac.wellcome.platform.reindexer.services._
import uk.ac.wellcome.utils.TryBackoff

object ReindexModule extends TwitterModule with TryBackoff {

  val targetTableName = flag[String](
    name = "reindex.target.tableName",
    help = "Reindex target table name")


  @Singleton
  @Provides
  def providesMiroReindexTargetService(dynamoDBClient: AmazonDynamoDB): ReindexTargetService[MiroTransformable] =
    new MiroReindexTargetService(dynamoDBClient, targetTableName())

  @Singleton
  @Provides
  def providesCalmReindexTargetService(dynamoDBClient: AmazonDynamoDB): ReindexTargetService[CalmTransformable] =
    new CalmReindexTargetService(dynamoDBClient, targetTableName())

  override def singletonStartup(injector: Injector) {
    info("Starting Reindexer module")

    val actorSystem = injector.instance[ActorSystem]
    val reindexService = targetTableName() match {
      case "MiroData" => injector.instance[ReindexService[MiroTransformable]]
      case "CalmData" => injector.instance[ReindexService[CalmTransformable]]
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
