package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.analyzers.EnglishLanguageAnalyzer
import com.sksamuel.elastic4s.http.ElasticDsl.{
  booleanField,
  intField,
  keywordField,
  mapping,
  objectField,
  textField
}
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import uk.ac.wellcome.elasticsearch.ElasticSearchIndex

trait CustomElasticSearchMapping {

  class SubsetOfFieldsWorksIndex(elasticClient: HttpClient, esType: String)
      extends ElasticSearchIndex {
    override val httpClient: HttpClient = elasticClient

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
        textField("title").fields(
          textField("english").analyzer(EnglishLanguageAnalyzer)),
        booleanField("visible"),
        objectField("mergeCandidates"),
        objectField("identifiers"),
        objectField("subjects"),
        keywordField("workType"),
        keywordField("description"),
        keywordField("physicalDescription"),
        keywordField("extent"),
        keywordField("lettering"),
        keywordField("createdDate"),
        keywordField("language"),
        keywordField("thumbnail"),
        keywordField("dimensions"),
        objectField("contributors"),
        objectField("genres"),
        objectField("items"),
        objectField("production"),
        keywordField("ontologyType")
      )

    override val mappingDefinition: MappingDefinition = mapping(esType)
      .dynamic(DynamicMapping.Strict)
      .as(rootIndexFields)
  }

}
