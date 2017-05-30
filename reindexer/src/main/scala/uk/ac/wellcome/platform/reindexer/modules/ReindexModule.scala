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

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ReindexModule extends TwitterModule {

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
    val tableName = targetTableName()

    info(s"Starting Reindexer module for $tableName")

    val actorSystem = injector.instance[ActorSystem]
    val reindexService = tableName match {
      case "MiroData" => injector.instance[ReindexService[MiroTransformable]]
      case "CalmData" => injector.instance[ReindexService[CalmTransformable]]
      case _ =>
        throw new RuntimeException(
          s"${tableName} is not a recognised reindexable table.")
    }

    val reindexJob = () => {
      reindexService.run.onComplete {
        case Success(_) => {
          info(s"ReindexModule job completed successfully.")
          agent.send("success")
        }
        case Failure(e) => {
          error(s"ReindexModule job failed!", e)
          agent.send("failure")
        }
      }
    }

    actorSystem.scheduler.scheduleOnce(0 millis)(reindexJob())
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")

    val actorSystem = injector.instance[ActorSystem]
    actorSystem.terminate()
  }
}
