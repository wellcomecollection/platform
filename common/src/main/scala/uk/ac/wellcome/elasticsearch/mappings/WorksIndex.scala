package uk.ac.wellcome.elasticsearch.mappings

import com.google.inject.Inject
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.twitter.inject.annotations.Flag
import org.elasticsearch.ResourceAlreadyExistsException
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class WorksIndex @Inject()(client: TcpClient,
                 @Flag("es.index") indexName: String,
                 @Flag("es.type") itemType: String) {

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
              objectField("work").fields(
                keywordField("type"),
                objectField("identifiers").fields(keywordField("source"),
                                                  keywordField("sourceId"),
                                                  keywordField("value")),
                textField("label").fields(
                  textField("english").analyzer(EnglishLanguageAnalyzer)),
                textField("description").fields(
                  textField("english").analyzer(EnglishLanguageAnalyzer)),
                textField("lettering").fields(
                  textField("english").analyzer(EnglishLanguageAnalyzer)),
                objectField("hasCreatedDate").fields(
                  textField("label"),
                  keywordField("type")
                ),
                objectField("hasCreator").fields(
                  textField("label"),
                  keywordField("type")
                )
              )
            )
        }
      }
}
