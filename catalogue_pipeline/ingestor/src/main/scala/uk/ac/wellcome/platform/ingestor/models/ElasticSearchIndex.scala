package uk.ac.wellcome.platform.ingestor.models

import com.sksamuel.elastic4s.http.ElasticDsl.createIndex
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.PutMappingDefinition
import org.elasticsearch.ResourceAlreadyExistsException
import com.sksamuel.elastic4s.http.ElasticDsl._
import org.elasticsearch.transport.RemoteTransportException
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import com.twitter.inject.Logging

import scala.concurrent.Future

trait ElasticSearchIndex extends Logging {
  val indexName: String
  val httpClient: HttpClient
  val mappingDefinition: PutMappingDefinition

  def create: Future[Unit] =
    httpClient
      .execute(createIndex(indexName))
      .recover {
        case e: RemoteTransportException
          if e.getCause.isInstanceOf[ResourceAlreadyExistsException] =>
          info(s"Index $indexName already exists")
        case _: ResourceAlreadyExistsException =>
          info(s"Index $indexName already exists")
        case e: Throwable =>
          error(s"Failed creating index $indexName", e)
          throw e
      }
      .flatMap { _ =>
        httpClient
          .execute {
            mappingDefinition
          }
          .recover {
            case e: Throwable =>
              error(s"Failed updating index $indexName", e)
              throw e
          }
      }
      .map { _ =>
        info("Index updated successfully")
      }
}
