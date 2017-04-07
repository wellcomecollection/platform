package uk.ac.wellcome.platform.ingestor

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.platform.ingestor.controllers._
import uk.ac.wellcome.platform.ingestor.modules._
import uk.ac.wellcome.finatra.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.ingestor Ingestor"
  override val modules = Seq(SQSConfigModule, SQSClientModule, AkkaModule, SQSReaderModule, SQSWorker, ElasticClientModule)

  private val esIndex = flag[String]("es.index", "records", "ES index name")
  private val esType = flag[String]("es.type", "item", "ES document type")

  private final val servicePrefix = flag(name = "service.prefix",
                                         default = "/ingestor",
                                         help = "API path prefix")

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
