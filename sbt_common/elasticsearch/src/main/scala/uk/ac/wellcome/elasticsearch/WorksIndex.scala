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
    keywordField("ontologyType"),
    keywordField("licenseType"),
    textField("label"),
    textField("url")
  )

  val sourceIdentifier = objectField("sourceIdentifier")
    .fields(
      keywordField("ontologyType"),
      keywordField("identifierScheme"),
      keywordField("value")
    )

  val identifiers = objectField("identifiers")
    .fields(
      keywordField("ontologyType"),
      keywordField("identifierScheme"),
      keywordField("value")
    )

  val workType = objectField("workType")
    .fields(
      keywordField("ontologyType"),
      keywordField("id"),
      keywordField("label")
    )

  def location(fieldName: String = "locations") =
    objectField(fieldName).fields(
      keywordField("type"),
      keywordField("ontologyType"),
      keywordField("locationType"),
      keywordField("label"),
      textField("url"),
      textField("credit"),
      license
    )

  def date(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType")
  )

  def concept(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType"),
    keywordField("type"),
    keywordField("qualifierType"),
    objectField("qualifiers").fields(
      textField("label"),
      keywordField("ontologyType"),
      keywordField("qualifierType")
    ),
    // Nested concept -- if qualified concept
    objectField("concept").fields(
      textField("label"),
      keywordField("ontologyType"),
      keywordField("qualifierType"),
      objectField("qualifiers").fields(
        textField("label"),
        keywordField("ontologyType"),
        keywordField("qualifierType")
      )
    )
  )

  def labelledTextField(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType")
  )

  def agent(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("type"),
    keywordField("prefix"),
    keywordField("numeration"),
    keywordField("ontologyType")
  )

  val items = objectField("items").fields(
    keywordField("canonicalId"),
    sourceIdentifier,
    identifiers,
    location(),
    booleanField("visible"),
    keywordField("ontologyType")
  )
  val language = objectField("language").fields(
    keywordField("id"),
    textField("label"),
    keywordField("ontologyType")
  )

  val rootIndexFields: Seq[FieldDefinition with Product with Serializable] =
    Seq(
      keywordField("canonicalId"),
      booleanField("visible"),
      keywordField("ontologyType"),
      intField("version"),
      sourceIdentifier,
      identifiers,
      workType,
      textField("title").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      textField("description").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      textField("physicalDescription").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      textField("extent").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      textField("lettering").fields(
        textField("english").analyzer(EnglishLanguageAnalyzer)),
      date("createdDate"),
      agent("creators"),
      concept("subjects"),
      concept("genres"),
      labelledTextField("placesOfPublication"),
      items,
      agent("publishers"),
      date("publicationDate"),
      language,
      location("thumbnail")
    )

  val mappingDefinition: MappingDefinition = mapping(rootIndexType)
    .dynamic(DynamicMapping.Strict)
    .as(rootIndexFields)
}
