package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.{FieldDefinition, ObjectField}

object WorksIndex {
  val license = objectField("license").fields(
    keywordField("id")
  )

  def sourceIdentifierFields = Seq(
    keywordField("ontologyType"),
    objectField("identifierType").fields(
      keywordField("id"),
      keywordField("label"),
      keywordField("ontologyType")
    ),
    keywordField("value")
  )

  val sourceIdentifier = objectField("sourceIdentifier")
    .fields(sourceIdentifierFields)

  val otherIdentifiers = objectField("otherIdentifiers")
    .fields(sourceIdentifierFields)

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
      objectField("locationType").fields(
        keywordField("id"),
        keywordField("label"),
        keywordField("ontologyType")
      ),
      keywordField("label"),
      textField("url"),
      textField("credit"),
      license
    )

  def date(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType")
  )

  val concept = Seq(
    textField("label"),
    keywordField("ontologyType"),
    keywordField("type")
  )

  val agent = Seq(
    textField("label"),
    keywordField("type"),
    keywordField("prefix"),
    keywordField("numeration"),
    keywordField("ontologyType")
  )

  val rootConcept = concept ++ agent

  def identified(fieldName: String, fields: Seq[FieldDefinition]): ObjectField =
    objectField(fieldName).fields(
      textField("type"),
      objectField("agent").fields(fields),
      keywordField("canonicalId"),
      objectField("sourceIdentifier").fields(sourceIdentifierFields),
      objectField("otherIdentifiers").fields(sourceIdentifierFields)
    )

  val subject: Seq[FieldDefinition] = Seq(
    textField("label"),
    keywordField("ontologyType"),
    identified("concepts", rootConcept)
  )

  def subjects: ObjectField = identified("subjects", subject)

  def genre(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType"),
    identified("concepts", concept)
  )

  def labelledTextField(fieldName: String) = objectField(fieldName).fields(
    textField("label"),
    keywordField("ontologyType")
  )

  def period(fieldName: String) = labelledTextField(fieldName)

  def items(fieldName: String) = objectField(fieldName).fields(
    keywordField("canonicalId"),
    sourceIdentifier,
    otherIdentifiers,
    keywordField("type"),
    objectField("agent").fields(location(), keywordField("ontologyType"))
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

  val production: ObjectField = objectField("production").fields(
    textField("label"),
    period("places"),
    identified("agents", agent),
    date("dates"),
    objectField("function").fields(concept),
    keywordField("ontologyType")
  )

  val mergeCandidates = objectField("mergeCandidates").fields(
    objectField("identifier").fields(sourceIdentifierFields),
    keywordField("reason")
  )

  val rootIndexFields: Seq[FieldDefinition with Product with Serializable] =
    Seq(
      keywordField("canonicalId"),
      keywordField("ontologyType"),
      intField("version"),
      sourceIdentifier,
      otherIdentifiers,
      mergeCandidates,
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
      subjects,
      genre("genres"),
      items("items"),
      items("itemsV1"),
      production,
      language,
      location("thumbnail"),
      textField("dimensions"),
      objectField("redirect")
        .fields(sourceIdentifier, keywordField("canonicalId")),
      keywordField("type"),
      booleanField("merged")
    )
}
