package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.http.ElasticDsl.{intField, keywordField, mapping, objectField}
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import uk.ac.wellcome.elasticsearch.ElasticsearchIndexBuilder

trait CustomElasticsearchMapping {

  object OnlyInvisibleWorksIndex extends ElasticsearchIndexBuilder {
    def buildMappingDefinition(rootIndexType: String): MappingDefinition = {
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

      mapping(rootIndexType)
        .dynamic(DynamicMapping.Strict)
        .as(rootIndexFields)
    }
  }
}
