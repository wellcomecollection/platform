package uk.ac.wellcome.platform.archive.registrar.http

import java.net.URL

import akka.http.scaladsl.Http
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.Future

class RegistrarHTTP(
  vhs: VersionedHybridStore[StorageManifest,
                            EmptyMetadata,
                            ObjectStore[StorageManifest]],
  httpServerConfig: HTTPServerConfig,
  contextURL: URL
) extends Logging {
  val router = new Router(
    vhs = vhs,
    contextURL = contextURL
  )

  val bindingFuture: Future[Http.ServerBinding] = Http()
    .bindAndHandle(router.routes, httpServerConfig.host, httpServerConfig.port)

  def run(): Future[Http.HttpTerminated] =
    bindingFuture
      .map(b => {
        info(s"Listening on ${httpServerConfig.host}:${httpServerConfig.port}")

        b
      })
      .flatMap(_.whenTerminated)
}
