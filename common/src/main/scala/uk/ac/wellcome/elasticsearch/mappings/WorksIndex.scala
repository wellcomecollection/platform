package uk.ac.wellcome.elasticsearch.mappings

import com.google.inject.Inject
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import org.elasticsearch.ResourceAlreadyExistsException
import org.elasticsearch.transport.RemoteTransportException
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class WorksIndex @Inject()(client: HttpClient,
                           @Flag("es.index") indexName: String,
                           @Flag("es.type") itemType: String)
    extends Logging {

  val license = objectField("license").fields(
    keywordField("type"),
    keywordField("licenseType"),
    textField("label"),
    textField("url")
  )

  val identifiers = objectField("identifiers")
    .fields(keywordField("identifierScheme"), keywordField("value"))

  def location(fieldName: String = "locations") =
    objectField(fieldName).fields(
      keywordField("type"),
      keywordField("locationType"),
      textField("url"),
      license
    )

  def date(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("type")
  )

  def labelledTextField(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("type")
  )

  val items = objectField("items").fields(
    keywordField("canonicalId"),
    identifiers,
    location(),
    keywordField("type")
  )

  val mappingDefinition = putMapping(indexName / itemType)
    .dynamic(DynamicMapping.Strict)
    .as(
      keywordField("canonicalId"),
      keywordField("type"),
      identifiers,
      textField("title").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      textField("description").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      textField("lettering").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      date("createdDate"),
      labelledTextField("creators"),
      labelledTextField("subjects"),
      labelledTextField("genres"),
      items,
      location("thumbnail")
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
