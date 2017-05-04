package uk.ac.wellcome.lib.elasticsearch

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.analyzers._

// TODO: This should have a test before any more code is written.
class RecordsIndex(
  client: ElasticClient,
  indexName: String
  ) {

  def create = client.execute {
    createIndex(indexName).mappings(
      mapping("item").as(
	keywordField("canonicalId"),
        objectField("unifiedItem").as(
          objectField("identifiers").as(
            keywordField("source"),
            keywordField("sourceId"),
            keywordField("value")
	  ),
          textField("label").fields(
            textField("english").analyzer(EnglishLanguageAnalyzer)
	  )
        )
      )
    )
  }
}
