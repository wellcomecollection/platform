package uk.ac.wellcome.platform.archive.progress_http.services

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives.{JavaUUID, complete, get, onSuccess}
import akka.http.scaladsl.server.Route
import io.circe.Printer
import io.swagger.annotations._
import javax.ws.rs.Path
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.display.{RequestDisplayIngest, ResponseDisplayIngest}
import uk.ac.wellcome.platform.archive.progress_http.ProgressStarter

@Api(value = "progress")
@Path("progress")
class ProgressService(
  monitor: ProgressTracker,
  progressStarter: ProgressStarter,
  httpServerConfig: HTTPServerConfig,
  contextURL: URL
) {
  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._
  implicit val printer = Printer.noSpaces.copy(dropNullValues = true)

  private def createLocationHeader(progress: Progress) =
    Location(s"${httpServerConfig.externalBaseURL}/${progress.id}")

  def routes: Route = {
    pathPrefix("progress") {
      createIngestRequest ~ getIngestRequest
    }
  }


  @Path("/{id}")
  @ApiOperation(httpMethod = "GET", value = "Returns an ingest request status", response = classOf[ResponseDisplayIngest])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", value = "The id of the request", paramType = "path", format = "UUID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Not Found Error"))
  )
  def getIngestRequest: Route = path(JavaUUID) { id: UUID =>
    get {
      onSuccess(monitor.get(id)) {
        case Some(progress) =>
          complete(ResponseDisplayIngest(progress, contextURL))
        case None =>
          complete(NotFound -> "Ingest request not found")
      }
    }
  }

  @ApiOperation(httpMethod = "POST", value = "Initialises an ingest request", code = 201, response = classOf[ResponseDisplayIngest], responseHeaders = Array(
    new ResponseHeader(name = "Location", description="The URL of the created ingest request", response = classOf[URL]) )
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "ingestRequest", required = true, dataTypeClass = classOf[RequestDisplayIngest], value = "The request to initialise", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Invalid request"))
  )
  def createIngestRequest: Route = {
    post {
      entity(as[RequestDisplayIngest]) { progressCreateRequest =>
        onSuccess(progressStarter.initialise(progressCreateRequest.toProgress)) {
          progress =>
            respondWithHeaders(List(createLocationHeader(progress))) {
              complete(Created -> ResponseDisplayIngest(progress, contextURL))
            }
        }
      }
    }
  }
}
