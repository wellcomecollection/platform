package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{createIndex, _}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.twitter.inject.Logging
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.client.ResponseException
import org.elasticsearch.transport.RemoteTransportException
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

trait ElasticSearchIndex extends Logging {
  val indexName: String
  val httpClient: HttpClient
  val mappingDefinition: MappingDefinition

  def create: Future[Unit] =
    httpClient
      .execute(createIndex(indexName).mappings {
        mappingDefinition
      })
      .recover {
        case e: ResponseException =>
          if(e.getCause.isInstanceOf[ResourceAlreadyExistsException]) {
            info(s"Index $indexName already exists")
          }
        case e: Throwable =>
          error(s"Failed creating index $indexName", e)
          throw e
      }
      .flatMap { _ =>
        httpClient
          .execute {
            putMapping(indexName / mappingDefinition.`type`)
              .as(mappingDefinition.fields)
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
