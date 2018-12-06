package uk.ac.wellcome.elasticsearch

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
  def create(indexName: String,
             mappingDefinitionBuilder: MappingDefinitionBuilder): Future[Unit] =
    create(
      indexName = indexName,
      mappingDefinition =
        mappingDefinitionBuilder.buildMappingDefinition(indexName)
    )

  private def create(indexName: String,
                     mappingDefinition: MappingDefinition): Future[Unit] =
    elasticClient
      .execute {
        createIndex(indexName)
          .mappings { mappingDefinition }

          // Because we have a relatively small number of records (compared
          // to what Elasticsearch usually expects), we can get weird results
          // if our records are split across multiple shards.
          //
          // e.g. searching for the same query multiple times gets varying results
          //
          // This forces all our records to be indexed into a single shard,
          // which should avoid this problem.
          //
          // If/when we index more records, we should revisit this.
          //
          .shards(1)
      }
      .flatMap { response: Response[CreateIndexResponse] =>
        if (response.isError) {
          if (response.error.`type` == "resource_already_exists_exception" || response.error.`type` == "index_already_exists_exception") {
            info(s"Index $indexName already exists")
            update(indexName, mappingDefinition = mappingDefinition)
          } else {
            Future.failed(
              throw new RuntimeException(
                s"Failed creating index $indexName: ${response.error}"
              )
            )
          }
        } else {
          Future.successful(response)
        }
      }
      .map { _ =>
        info("Index updated successfully")
      }

  private def update(indexName: String,
                     mappingDefinition: MappingDefinition): Future[Unit] =
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
      .map { response: Response[PutMappingResponse] =>
        if (response.isError) {
          throw new RuntimeException(s"Failed updating index: $response")
        }
        response
      }
      .map { _ =>
        info("Successfully applied new mapping")
      }
}
