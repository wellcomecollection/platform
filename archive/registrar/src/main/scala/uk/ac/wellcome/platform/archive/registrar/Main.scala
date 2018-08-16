package uk.ac.wellcome.platform.archive.registrar

import akka.Done
import uk.ac.wellcome.platform.archive.common.app.WellcomeApp
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.registrar.modules.{
  AppConfigModule,
  ConfigModule,
  VHSModule
}

object Main extends WellcomeApp[RegistrarWorker, Done] with RegistrarModules {
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
