package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import com.google.inject.name.Named
import com.google.inject.{Guice, Inject}
//import uk.ac.wellcome.platform.archiver.modules.{AkkaModule, AkkaS3ClientModule}

case class Config @Inject()(@Named("conf") value: String)

object Main extends App {
  val system: ActorSystem = ActorSystem("archiver")
  val injector = Guice.createInjector()

  println(args.toList)

  //  val s3Client = injector.getInstance(S3Client.getClass)

  try {
    val dummy = injector.getInstance(classOf[Config])

    println(s"hi ${dummy.value}")

  } finally {
    system.terminate
  }
}