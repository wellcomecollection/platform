package uk.ac.wellcome.platform.reindexer.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.agent.Agent
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Provides
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.models.{CalmTransformable, MiroTransformable}
import uk.ac.wellcome.platform.reindexer.services._
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.TryBackoff
import scala.concurrent.duration._

object ReindexModule extends TwitterModule with TryBackoff {

  val targetTableName = flag[String](name = "reindex.target.tableName",
                                     help = "Reindex target table name")

  val agent = Agent("working")

  @Singleton
  @Provides
  def providesMiroReindexTargetService(
    dynamoDBClient: AmazonDynamoDB): ReindexTargetService[MiroTransformable] =
    new MiroReindexTargetService(dynamoDBClient, targetTableName())

  @Singleton
  @Provides
  def providesCalmReindexTargetService(
    dynamoDBClient: AmazonDynamoDB): ReindexTargetService[CalmTransformable] =
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

    // Wait 30 seconds before starting to ensure healthcheck registers ok
    actorSystem.scheduler.scheduleOnce(30 seconds)(
      reindexService.run.onComplete(_ => {
        agent.send("done") }))
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")

    val actorSystem = injector.instance[ActorSystem]
    actorSystem.terminate()
  }
}
