package uk.ac.wellcome.platform.reindexer.modules

import javax.inject.Singleton

import akka.actor.ActorSystem
import com.google.inject.Provides
import com.gu.scanamo.DynamoFormat
import com.twitter.app.Flag
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  MiroTransformable,
  Reindexable
}
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
  val targetReindexShard: Flag[String] = flag[String](
    name = "reindex.target.reindexShard",
    help = "Reindex shard to use",
    default = "default"
  )

  @Singleton
  @Provides
  def providesReindexService(
    injector: Injector): ReindexService[Reindexable[String]] = {
    val tableName = targetTableName()

    tableName match {
      case "MiroData" => createReindexService[MiroTransformable](injector)
      case "CalmData" => createReindexService[CalmTransformable](injector)
      case _ =>
        throw new RuntimeException(
          s"$tableName is not a recognised reindexable table.")
    }
  }

  override def singletonStartup(injector: Injector) {
    val tableName = targetTableName()

    info(s"Starting Reindexer module for $tableName")

    ReindexStatus.init()

    val actorSystem = injector.instance[ActorSystem]
    val reindexService = injector.instance[ReindexService[Reindexable[String]]]

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

  private def createReindexService[T <: Reindexable[String]](
    injector: Injector)(implicit dynamoFormat: DynamoFormat[T],
                        evidence: Manifest[ReindexTargetService[T]]) = {
    new ReindexService[T](injector.instance[ReindexTrackerService],
                          injector.instance[ReindexTargetService[T]],
                          injector.instance[MetricsSender])
  }

  override def singletonShutdown(injector: Injector) {
    info("Terminating Reindexer")

    val actorSystem = injector.instance[ActorSystem]
    actorSystem.terminate()
  }
}
