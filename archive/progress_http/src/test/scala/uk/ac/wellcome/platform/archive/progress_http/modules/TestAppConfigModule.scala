package uk.ac.wellcome.platform.archive.progress_http.modules

import com.google.inject.{AbstractModule, Provides}
import uk.ac.wellcome.monitoring.MetricsConfig
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressMonitorConfig
import uk.ac.wellcome.platform.archive.progress_http.models.{
  HttpServerConfig,
  ProgressHttpConfig
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table

import scala.concurrent.duration._

class TestAppConfigModule(
  progressTable: Table,
  serverConfig: HttpServerConfig
) extends AbstractModule {

  @Provides
  def providesAppConfig = {
    val cloudwatchClientConfig = CloudwatchClientConfig(
      region = "localhost",
      endpoint = Some("localhost")
    )

    val metricsConfig = MetricsConfig(
      namespace = "namespace",
      flushInterval = 60 seconds
    )

    val archiveProgressMonitorConfig = ProgressMonitorConfig(
      DynamoConfig(
        table = progressTable.name,
        index = progressTable.index
      ),
      DynamoClientConfig(
        accessKey = Some("access"),
        secretKey = Some("secret"),
        region = "localhost",
        endpoint = Some("http://localhost:45678")
      )
    )

    ProgressHttpConfig(
      cloudwatchClientConfig,
      archiveProgressMonitorConfig,
      metricsConfig,
      serverConfig
    )
  }
}
