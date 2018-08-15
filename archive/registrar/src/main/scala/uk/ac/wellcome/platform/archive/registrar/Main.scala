package uk.ac.wellcome.platform.archive.registrar

import akka.Done
import com.google.inject.{AbstractModule, Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{AppConfigModule, ConfigModule}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Main extends WellcomeApp[RegistrarWorker, Done] with RegistrarModules {
  val worker = injector.getInstance(classOf[RegistrarWorker])
}

trait RegistrarModules {
  val configuredModules = List(
    ConfigModule,
    VHSModule,
    AkkaModule,
    AkkaS3ClientModule,
    CloudWatchClientModule,
    SQSClientModule,
    SNSAsyncClientModule,
    DynamoClientModule,
    MessageStreamModule
  )

}

trait WellcomeApp[W <: Worker[R], R] extends App with InjectedModules {
  val worker: W
  val appConfigModule = new AppConfigModule(args)

  try {
    info(s"Starting worker.")
    val app = worker.run()
    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}

trait InjectedModules extends Logging {
  val configuredModules: List[AbstractModule]
  val appConfigModule: AbstractModule

  lazy val modules: List[AbstractModule] = appConfigModule :: configuredModules
  lazy val injector: Injector = Guice.createInjector(modules.asJava)
}

trait Worker[T <: Any] {
  def run(): Future[T]
}

