package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import com.google.inject._
import uk.ac.wellcome.platform.archiver.models.AppConfig
import uk.ac.wellcome.platform.archiver.modules.AppConfigModule
import uk.ac.wellcome.platform.archiver.modules.{AkkaModule, AkkaS3ClientModule}

object Main extends App {
  lazy val appConfig = new AppConfig(args)
  lazy val injector = Guice.createInjector(
    new AppConfigModule(appConfig),
    AkkaModule,
    AkkaS3ClientModule
  )

  val actorSystem = injector.getInstance(classOf[ActorSystem])

  try {

    println("hi")

  } finally {
    actorSystem.terminate
  }
}