package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import com.sksamuel.elastic4s.http.ElasticDsl.{createIndex, _}
import com.sksamuel.elastic4s.http.index.mappings.PutMappingResponse
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import grizzled.slf4j.Logging
import org.elasticsearch.client.ResponseException

import scala.concurrent.{ExecutionContext, Future}

trait ElasticsearchIndex extends Logging {
  val elasticClient: ElasticClient
  val mappingDefinition: MappingDefinition
  implicit val ec: ExecutionContext

  def create(indexName: String): Future[Unit] =
    elasticClient
      .execute(createIndex(indexName).mappings {
        mappingDefinition
      })
      .recoverWith {
        case e: ResponseException
            if e.getMessage.contains("index_already_exists_exception") =>
          info(s"Index $indexName already exists")
          update(indexName)
        case e: Throwable =>
          error(s"Failed creating index $indexName", e)
          Future.failed(e)
      }
      .map { _ =>
        info("Index updated successfully")
      }

  private def update(indexName: String): Future[Response[PutMappingResponse]] =
    elasticClient
      .execute {
        putMapping(indexName / mappingDefinition.`type`)
          .dynamic(mappingDefinition.dynamic.getOrElse(DynamicMapping.Strict))
          .as(mappingDefinition.fields)
      }
      .recover {
        case e: Throwable =>
          error(s"Failed updating index $indexName", e)
          throw e
      }
}
