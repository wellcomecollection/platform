package uk.ac.wellcome.platform.archive.progress_http.modules

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.google.inject.{AbstractModule, Provides}
import com.google.inject.name.Named
import uk.ac.wellcome.platform.archive.progress_http.models.HttpServerConfig

import scala.concurrent.Future

object HttpStreamModule extends AbstractModule {

  @Provides
  @Named("appFuture")
  def providesGraph(
                     sink: Sink[Http.IncomingConnection, Future[Done]],
                     source: Source[Http.IncomingConnection, Future[Http.ServerBinding]]
                   )(implicit materializer: ActorMaterializer) = {

    source.runWith(sink)
  }

  @Provides
  def providesServerSink(handler: HttpRequest => HttpResponse)(implicit materializer: ActorMaterializer): Sink[Http.IncomingConnection, Future[Done]] = {
    Sink.foreach[Http.IncomingConnection] { in =>
      in.handleWith {
        Flow[HttpRequest].map(handler)
      }
    }
  }

  @Provides
  def providesServerSource(serverConfig: HttpServerConfig)(implicit actorSystem: ActorSystem): Source[Http.IncomingConnection, Future[Http.ServerBinding]] = {
    Http().bind(
      interface = serverConfig.host,
      port = serverConfig.port
    )
  }
}
