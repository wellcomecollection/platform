package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.http.ElasticDsl.{
  intField,
  keywordField,
  mapping,
  objectField
}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import uk.ac.wellcome.elasticsearch.ElasticsearchIndex

import scala.concurrent.ExecutionContext

trait CustomElasticsearchMapping {

  class OnlyInvisibleWorksIndex(
    elasticClient: HttpClient,
    documentType: String)(implicit val ec: ExecutionContext)
      extends ElasticsearchIndex {
    val httpClient: HttpClient = elasticClient

    def sourceIdentifierFields = Seq(
      keywordField("ontologyType"),
      objectField("identifierType").fields(
        keywordField("id"),
        keywordField("label"),
        keywordField("ontologyType")
      ),
      keywordField("value")
    )

    val rootIndexFields: Seq[FieldDefinition with Product with Serializable] =
      Seq(
        keywordField("canonicalId"),
        intField("version"),
        objectField("sourceIdentifier")
          .fields(sourceIdentifierFields),
        keywordField("type")
      )

    override val mappingDefinition: MappingDefinition = mapping(documentType)
      .dynamic(DynamicMapping.Strict)
      .as(rootIndexFields)
  }

}
