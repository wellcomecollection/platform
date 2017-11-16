package uk.ac.wellcome.platform.sierra_dynamo.utils

import java.io.File

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest._

trait SierraWireMock extends BeforeAndAfterAll { this: Suite =>

  lazy val recordingMode = false
  lazy val oauthKey ="key"
  lazy val oauthSecret= "secret"

  private val port = 8089
  private val host = "localhost"
  private val wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().usingFilesUnderDirectory(determineMappingsFolder).port(port))

  private val sierraUrl = "https://libsys.wellcomelibrary.org/iii/sierra-api/v3"

  final val sierraWireMockUrl = s"http://$host:$port"

  override def beforeAll(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(host, port)
    if(recordingMode){
      println("Configuring recording")
      wireMockServer.startRecording(recordSpec()
        .forTarget(sierraUrl)
        .ignoreRepeatRequests()
        .captureHeader("Authentication"))
      stubFor(proxyAllTo(sierraUrl).atPriority(1))
    }

    super.beforeAll()
  }

  override def afterAll(): Unit = {
    if(recordingMode) {
      wireMockServer.stopRecording
      wireMockServer.saveMappings()
    }
    wireMockServer.stop()

    super.afterAll()
  }


  private def determineMappingsFolder = {
    val file = new File(".").getAbsoluteFile
    val files = file.listFiles().filter(_.isDirectory)
    val strings = files.map(_.getName)
    if (files.exists((f: File) =>f.getName == "sierra_to_dynamo")) {
      "sierra_to_dynamo/src/test/resources"
    }
    else "src/test/resources"
  }
}
