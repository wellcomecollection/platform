package uk.ac.wellcome.elasticsearch

import com.google.inject.Inject
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}
import com.sksamuel.elastic4s.mappings.dynamictemplate.DynamicMapping
import grizzled.slf4j.Logging

class WorksIndex @Inject()(client: HttpClient, itemType: String)
    extends ElasticSearchIndex
    with Logging {

  val rootIndexType = itemType

  val httpClient: HttpClient = client

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

  def subject(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType"),
    identified("concepts", concept)
  )

  def genre(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType"),
    identified("concepts", concept)
  )

  val concept = Seq(
    textField("label"),
    keywordField("ontologyType"),
    keywordField("type")
  )

  def labelledTextField(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType")
  )

  def identified(fieldName: String, fields: Seq[FieldDefinition]) =
    objectField(fieldName).fields(
      textField("type"),
      objectField("agent").fields(fields),
      keywordField("canonicalId"),
      identifiers
    )

  val agent = Seq(
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

  val contributors = objectField("contributors").fields(
    identified("agent", agent),
    objectField("roles").fields(
      textField("label"),
      keywordField("ontologyType")
    ),
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
      contributors,
      subject("subjects"),
      genre("genres"),
      labelledTextField("placesOfPublication"),
      items,
      identified("publishers", agent),
      date("publicationDate"),
      language,
      location("thumbnail"),
      textField("dimensions")
    )

  val mappingDefinition: MappingDefinition = mapping(rootIndexType)
    .dynamic(DynamicMapping.Strict)
    .as(rootIndexFields)
}
