package uk.ac.wellcome.elasticsearch.mappings

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.DynamicMapping
import org.elasticsearch.ResourceAlreadyExistsException
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class WorksIndex(client: ElasticClient, indexName: String, itemType: String) {

  def create =
    client
      .execute(createIndex(indexName))
      .recover { case e: ResourceAlreadyExistsException => () }
      .flatMap { _ =>
        client.execute {
          putMapping(indexName / itemType)
            .dynamic(DynamicMapping.Strict)
            .as(
              keywordField("canonicalId"),
              objectField("work").as(
                keywordField("type"),
                objectField("identifiers").as(keywordField("source"),
                                              keywordField("sourceId"),
                                              keywordField("value")),
                textField("label").fields(
                  textField("english").analyzer(EnglishLanguageAnalyzer)),
                textField("description").fields(
                  textField("english").analyzer(EnglishLanguageAnalyzer)),
                textField("lettering").fields(
                  textField("english").analyzer(EnglishLanguageAnalyzer))
              )
            )
        }
      }
}
