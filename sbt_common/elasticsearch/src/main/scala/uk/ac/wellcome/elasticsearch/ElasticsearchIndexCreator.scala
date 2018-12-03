package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.http.{ElasticClient, Response}
import com.sksamuel.elastic4s.http.ElasticDsl.{createIndex, _}
import com.sksamuel.elastic4s.http.index.CreateIndexResponse
import com.sksamuel.elastic4s.http.index.mappings.PutMappingResponse
import com.sksamuel.elastic4s.mappings.MappingDefinition
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchIndexCreator(elasticClient: ElasticClient)(
  implicit ec: ExecutionContext)
    extends Logging {
  def create(index: Index,
             mappingDefinition: MappingDefinition): Future[Unit] =
    elasticClient
      .execute {
        createIndex(index.name).mappings {
          mappingDefinition
        }
      }
      .map { response: Response[CreateIndexResponse] =>
        if (response.isError) {
          if (response.error.`type` == "resource_already_exists_exception") {
            info(s"Index $indexName already exists")
            update(index, mappingDefinition = mappingDefinition)
          } else {
            Future.failed(
              throw new RuntimeException(
                s"Failed creating index $index: ${response.error}"
              )
            )
          }
        } else {
          response
        }
      }
      .map { _ =>
        info("Index updated successfully")
      }

  private def update(index: Index, mappingDefinition: MappingDefinition)
    : Future[Response[PutMappingResponse]] =
    elasticClient
      .execute {
        putMapping(index.name / mappingDefinition.`type`)
          .dynamic(mappingDefinition.dynamic.getOrElse(DynamicMapping.Strict))
          .as(mappingDefinition.fields)
      }
      .recover {
        case e: Throwable =>
          error(s"Failed updating index $index", e)
          throw e
      }
      .map { response: Response[PutMappingResponse] =>
        if (response.isError) {
          throw new RuntimeException(s"Failed updating index $index: $response")
        }
        response
      }
}
