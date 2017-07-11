package uk.ac.wellcome.elasticsearch.mappings

import com.google.inject.Inject
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse
import org.elasticsearch.transport.RemoteTransportException
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class WorksIndex @Inject()(client: HttpClient,
                           @Flag("es.index") indexName: String,
                           @Flag("es.type") itemType: String)
    extends Logging {

  val mappingDefinition = putMapping(indexName / itemType)
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
        objectField("createdDate").fields(
          textField("label"),
          keywordField("type")
        ),
        objectField("creators").fields(
          textField("label"),
          keywordField("type")
        )
      )
    )

  def create: Future[Unit] =
    client
      .execute(createIndex(indexName))
      .recover {
        case e: RemoteTransportException
            if e.getCause.isInstanceOf[ResourceAlreadyExistsException] =>
          info(s"Index $indexName already exists")
        case _: ResourceAlreadyExistsException =>
          info(s"Index $indexName already exists")
        case e: Throwable =>
          error(s"Failed creating index $indexName", e)
          throw e
      }
      .flatMap { _ =>
        client
          .execute {
            mappingDefinition
          }
          .recover {
            case e: Throwable =>
              error(s"Failed updating index $indexName", e)
              throw e
          }
      }
      .map { _ =>
        info("Index updated successfully")
      }
}
