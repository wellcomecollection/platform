package uk.ac.wellcome.platform.archive.registrar.http.config
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.registrar.common.modules.HybridStoreConfig

case class RegistrarHttpConfig(
  hybridStoreConfig: HybridStoreConfig,
  httpServerConfig: HttpServerConfig,
)
