package uk.ac.wellcome.elasticsearch

import com.google.inject.Inject
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag

class WorksIndex @Inject()(client: HttpClient,
                           @Flag("es.index") name: String,
                           @Flag("es.type") itemType: String)
    extends ElasticSearchIndex
    with Logging {

  val rootIndexType = itemType

  val httpClient: HttpClient = client
  val indexName = name

  val license = objectField("license").fields(
    keywordField("type"),
    keywordField("licenseType"),
    textField("label"),
    textField("url")
  )

  val sourceIdentifier = objectField("sourceIdentifier")
    .fields(
      keywordField("type"),
      keywordField("identifierScheme"),
      keywordField("value")
    )

  val identifiers = objectField("identifiers")
    .fields(
      keywordField("type"),
      keywordField("identifierScheme"),
      keywordField("value")
    )

  def location(fieldName: String = "locations") =
    objectField(fieldName).fields(
      keywordField("type"),
      keywordField("locationType"),
      keywordField("label"),
      textField("url"),
      textField("credit"),
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
    sourceIdentifier,
    identifiers,
    location(),
    booleanField("visible"),
    keywordField("type")
  )
  val publishers = objectField("publishers").fields(
    textField("label"),
    keywordField("type")
  )

  val rootIndexFields: Seq[FieldDefinition with Product with Serializable] =
    Seq(
      keywordField("canonicalId"),
      booleanField("visible"),
      keywordField("type"),
      intField("version"),
      sourceIdentifier,
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
      publishers,
      location("thumbnail")
    )

  val mappingDefinition: MappingDefinition = mapping(rootIndexType)
    .dynamic(DynamicMapping.Strict)
    .as(rootIndexFields)
}
