package uk.ac.wellcome.platform.transformer.modules

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{ActorSystem, Props}
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.twitter.inject.{Injector, Logging, TwitterModule}

import uk.ac.wellcome.platform.transformer.modules._


object SQSWorker extends TwitterModule {
  override val modules = Seq(AkkaModule)

  override def singletonStartup(injector: Injector) {
    info("Starting SQS worker")

    val system = injector.instance[ActorSystem]

    system.scheduler.scheduleOnce(
      Duration.create(50, TimeUnit.MILLISECONDS),
      new Thread { println("boof") }
    )

  }

  override def singletonShutdown(injector: Injector) {
    info("Shutting down SQS worker")
    val system = injector.instance[ActorSystem]
    system.terminate()
  }
}
