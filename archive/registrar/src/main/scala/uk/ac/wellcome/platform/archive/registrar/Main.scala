package uk.ac.wellcome.platform.archive.registrar

import akka.Done
import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.platform.archive.common.app.WellcomeApp
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{AppConfigModule, ConfigModule, VHSModule}

import scala.collection.JavaConverters._

object Main extends WellcomeApp[RegistrarWorker, Done] with RegistrarModules {
  override val injector: Injector = Guice.createInjector(modules.asJava)

  lazy val appConfigModule = new AppConfigModule(args)
  lazy val worker = injector.getInstance(classOf[RegistrarWorker])
}

trait RegistrarModules {
  lazy val configuredModules = List(
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
