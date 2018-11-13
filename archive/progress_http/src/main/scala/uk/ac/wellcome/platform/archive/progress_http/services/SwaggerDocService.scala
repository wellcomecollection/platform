package uk.ac.wellcome.platform.archive.progress_http.services
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[ProgressService])
  override val host = "localhost:9001"
  override def basePath = ""
  override def apiDocsPath = "progress"
  override val info = Info(version = "v1")
}
