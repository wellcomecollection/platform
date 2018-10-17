package uk.ac.wellcome.platform.archive.registrar.http
import com.google.inject.{Guice, Injector}
import uk.ac.wellcome.platform.archive.common.modules.{
  AkkaModule,
  DynamoClientModule
}
import uk.ac.wellcome.platform.archive.registrar.common.modules.VHSModule
import uk.ac.wellcome.platform.archive.registrar.http.modules.{
  AkkaHttpApp,
  AppConfigModule,
  ConfigModule
}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App with AkkaHttpApp {
  override val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
    VHSModule,
    AkkaModule,
    DynamoClientModule
  )

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
