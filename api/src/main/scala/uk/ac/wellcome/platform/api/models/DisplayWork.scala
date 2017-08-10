package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.get.GetResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil

@ApiModel(
  value="Work",
  description="An individual work such as a text, archive item or picture; or a grouping of individual works (so, for instance, an archive collection counts as a work, as do all the series and individual files within it).  Each work may exist in multiple instances (e.g. copies of the same book).  N.B. this is not synonymous with \\\"work\\\" as that is understood in the International Federation of Library Associations and Institutions' Functional Requirements for Bibliographic Records model (FRBR) but represents something lower down the FRBR hierarchy, namely manifestation. Groups of related items are also included as works because they have similar properties to the individual ones."
)
case class DisplayWork(
  id: String,
  @ApiModelProperty(value = "The title or other short label of a work, including labels not present in the actual work or item but applied by the cataloguer for the purposes of search or description.") title: String,
  @ApiModelProperty(dataType = "String", value = "A description given to a thing.") description: Option[String] = None,
  @ApiModelProperty(dataType = "String", value = "Recording written text on a (usually visual) work.") lettering: Option[String] = None,
  @ApiModelProperty(dataType = "uk.ac.wellcome.models.Period", value = "Relates the creation of a work to a date, when the date of creation does not cover a range.") createdDate: Option[Period] = None,
  @ApiModelProperty(value = "Relates a work to its author, compiler, editor, artist or other entity responsible for its coming into existence in the form that it has.") creators: List[Agent] = List(),
  @ApiModelProperty(value = "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem.") identifiers: Option[List[DisplayIdentifier]] = None) {
  @ApiModelProperty(value = "A type of thing") @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWork {

  def apply(hit: SearchHit): DisplayWork =
    apply(hit, includes = WorksIncludes())

  def apply(hit: SearchHit, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(hit.sourceAsString, includes)
  }

  def apply(got: GetResponse, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(got.sourceAsString, includes)
  }

  private def jsonToDisplayWork(document: String, includes: WorksIncludes) = {
    val identifiedWork =
      JsonUtil.fromJson[IdentifiedWork](document).get

    DisplayWork(
      id = identifiedWork.canonicalId,
      title = identifiedWork.work.title,
      description = identifiedWork.work.description,
      lettering = identifiedWork.work.lettering,
      createdDate = identifiedWork.work.createdDate,
      // Wrapping this in Option to catch null value from Jackson
      creators = Option(identifiedWork.work.creators).getOrElse(Nil),
      identifiers =
        if (includes.identifiers)
          Some(identifiedWork.work.identifiers.map(DisplayIdentifier(_)))
        else None
    )
  }
}

case class DisplayIdentifier(source: String, name: String, value: String) {
  @JsonProperty("type")
  val ontologyType: String = "Identifier"
}

object DisplayIdentifier {
  def apply(sourceIdentifier: SourceIdentifier): DisplayIdentifier =
    DisplayIdentifier(source = sourceIdentifier.source,
                      name = sourceIdentifier.sourceId,
                      value = sourceIdentifier.value)
}
