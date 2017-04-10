package uk.ac.wellcome.platform.idminter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.idminter.controllers.ManagementController
import uk.ac.wellcome.platform.idminter.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.id_minter IdMinter"
  override val modules = Seq(AkkaModule,
                             IdMinterModule,
                             SQSClientModule,
                             SQSConfigModule,
                             SQSReaderModule,
                             DynamoConfigModule,
                             AmazonDynamoDBModule,
                             SNSConfigModule,
                             SNSClientModule)

  private final val servicePrefix = flag(name = "service.prefix",
                                         default = "/idminter",
                                         help = "API path prefix")

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
