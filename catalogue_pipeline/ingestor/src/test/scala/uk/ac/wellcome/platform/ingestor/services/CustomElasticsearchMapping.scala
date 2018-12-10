package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.http.ElasticDsl.{
  intField,
  keywordField,
  objectField
}
import com.sksamuel.elastic4s.mappings.FieldDefinition

trait CustomElasticsearchMapping {

  object OnlyInvisibleWorksIndex {
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
  }
}
