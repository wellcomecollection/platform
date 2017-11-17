package uk.ac.wellcome.platform.sierra_dynamo.utils

import java.io.File

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest._

trait SierraWireMock extends BeforeAndAfterAll { this: Suite =>

  lazy val oauthKey = sys.env.getOrElse("SIERRA_KEY", "key")
  lazy val oauthSecret = sys.env.getOrElse("SIERRA_SECRET", "secret")

  private val port = 8089
  private val host = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().usingFilesUnderDirectory(determineMappingsFolder).port(port))

  private val sierraUrl = "https://libsys.wellcomelibrary.org/iii/sierra-api/v3"

  final val sierraWireMockUrl = s"http://$host:$port"

  wireMockServer.start()
  WireMock.configureFor(host, port)

  stubFor(proxyAllTo(sierraUrl).atPriority(100))

  override def afterAll(): Unit = {
    wireMockServer.snapshotRecord(
      recordSpec()
        .forTarget(sierraUrl)
        .captureHeader("Authorization")
    )
    wireMockServer.stop()

    super.afterAll()
  }


  private def determineMappingsFolder = {
//    Horrible hack to make tests run in intelliJ understand where to find the mappings folder
    val file = new File(".").getAbsoluteFile
    val files = file.listFiles().filter(_.isDirectory)
    if (files.exists((f: File) =>f.getName == "sierra_to_dynamo")) {
      "sierra_to_dynamo/src/test/resources"
    }
    else "src/test/resources"
  }
}
