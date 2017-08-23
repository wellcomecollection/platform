package uk.ac.wellcome.platform.reindexer.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Provides
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.CalmTransformable
import uk.ac.wellcome.models.transformable.miro.MiroTransformable
import uk.ac.wellcome.platform.reindexer.models._
import uk.ac.wellcome.platform.reindexer.services._
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.TryBackoff

import scala.util.{Failure, Success}

object ReindexModule extends TwitterModule with TryBackoff {

  override lazy val continuous: Boolean = false

  val targetTableName: Flag[String] = flag[String](
    name = "reindex.target.tableName",
    help = "Reindex target table name")

  @Singleton
  @Provides
  def providesMiroReindexTargetService(
    dynamoDBClient: AmazonDynamoDB,
    metricsSender: MetricsSender): ReindexTargetService[MiroTransformable] =
    new MiroReindexTargetService(dynamoDBClient,
                                 targetTableName(),
                                 metricsSender)

  @Singleton
  @Provides
  def providesCalmReindexTargetService(
    dynamoDBClient: AmazonDynamoDB,
    metricsSender: MetricsSender): ReindexTargetService[CalmTransformable] =
    new CalmReindexTargetService(dynamoDBClient,
                                 targetTableName(),
                                 metricsSender)

  override def singletonStartup(injector: Injector) {
    val tableName = targetTableName()

    info(s"Starting Reindexer module for $tableName")

    ReindexStatus.init()

    val actorSystem = injector.instance[ActorSystem]
    val reindexService = tableName match {
      case "MiroData" => injector.instance[ReindexService[MiroTransformable]]
      case "CalmData" => injector.instance[ReindexService[CalmTransformable]]
      case _ =>
        throw new RuntimeException(
          s"$tableName is not a recognised reindexable table.")
    }

    val reindexJob = () => {
      val future = reindexService.run
      future.onComplete {
        case Success(_) =>
          info(s"ReindexModule job completed successfully.")
          ReindexStatus.succeed()
        case Failure(e) =>
          error(s"ReindexModule job failed!", e)
          ReindexStatus.fail()
      }
      future
    }

    run(() => reindexJob(), actorSystem)
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")

    val actorSystem = injector.instance[ActorSystem]
    actorSystem.terminate()
  }
}
