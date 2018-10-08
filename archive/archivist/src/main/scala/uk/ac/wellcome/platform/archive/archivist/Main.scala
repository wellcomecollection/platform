package uk.ac.wellcome.platform.archive.archivist

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Guice, Injector, Key}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.flow._
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveError
import uk.ac.wellcome.platform.archive.archivist.modules.{BagUploaderConfig, BagUploaderConfigModule}
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{IngestBagRequest, NotificationMessage}
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorClientModule

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Main extends App { //with Archivist {
  val app = new ArchivistApp()
  app.run()

}
//  override val injector: Injector = Guice.createInjector(
//    TypesafeConfigModule,
//    AkkaModule,
//    S3ClientModule,
//    CloudWatchClientModule,
//    SnsClientModule,
//    MessageStreamModule,
//    ProgressMonitorClientModule,
//    BagUploaderConfigModule
//  )
//
//  try {
//    info(s"Starting worker.")
//
//    val app = run()
//
//    Await.result(app, Duration.Inf)
//  } catch {
//    case e: Throwable =>
//      error("Fatal error:", e)
//  } finally {
//    info(s"Terminating worker.")
//  }
}

class ArchivistApp() extends WellcomeApp {
  val configModule = TypesafeConfigModule

  val modules = List(
    AkkaModule,
    S3ClientModule,
    CloudWatchClientModule,
    SnsClientModule,
    MessageStreamModule,
    ProgressMonitorClientModule,
    BagUploaderConfigModule
  )

  def run[Done](injector: Injector): Future[Done] = {

    implicit val amazonS3: AmazonS3 = injector.getInstance(classOf[AmazonS3])

    implicit val snsClient: AmazonSNS =
      injector.getInstance(classOf[AmazonSNS])
    implicit val actorSystem: ActorSystem =
      injector.getInstance(classOf[ActorSystem])
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val decider: Supervision.Decider = {
      case e => {
        error("Stream failure", e)
        Supervision.Resume
      }
    }

    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
    )

    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Unit]])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])
    val snsRegistrarConfig = injector.getInstance(
      Key.get(classOf[SNSConfig], Names.named("registrarSnsConfig")))
    val snsProgressConfig = injector.getInstance(
      Key.get(classOf[SNSConfig], Names.named("progressSnsConfig")))

    debug(s"registrar topic: $snsRegistrarConfig")
    debug(s"progress topic: $snsProgressConfig")

    val workFlow =
      Flow[NotificationMessage]
        .log("notification message")
        .via(
          NotificationMessageFlow(
            bagUploaderConfig.parallelism,
            snsClient,
            snsProgressConfig))
        .log("download zip")
        .via(
          ZipFileDownloadFlow(bagUploaderConfig.parallelism, snsProgressConfig))
        .log("archiving zip")
        .via(
          FoldEitherFlow[
            ArchiveError[IngestBagRequest],
            ZipFileDownloadComplete,
            Unit
            ](ifLeft = _ => ())(
            ifRight = ArchiveAndNotifyRegistrarFlow(
              bagUploaderConfig,
              snsProgressConfig,
              snsRegistrarConfig)))

    messageStream.run(
      "archivist", workFlow)
  }

}

trait WellcomeApp extends Logging {
  val configModule: Configurable
  val modules: List[AbstractModule]

  val injector: Injector =
    Guice.createInjector(
      configModule :: modules : _*)

  def run[T](): Future[T]

  try {
    info(s"Starting service.")

    val app = run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating service.")
  }
}