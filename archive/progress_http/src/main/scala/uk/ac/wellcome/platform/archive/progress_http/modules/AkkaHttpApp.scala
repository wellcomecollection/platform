package uk.ac.wellcome.platform.archive.progress_http.modules

import akka.Done
import com.google.inject.{Injector, Key, TypeLiteral}
import com.google.inject.name.Names
import grizzled.slf4j.Logging

import scala.concurrent.Future

trait AkkaHttpApp extends Logging {
  val injector: Injector
  def run() = {
    injector.getInstance(
      Key.get(
        new TypeLiteral[Future[Done]]() {},
        Names.named("appFuture")
      )
    )
  }
}
