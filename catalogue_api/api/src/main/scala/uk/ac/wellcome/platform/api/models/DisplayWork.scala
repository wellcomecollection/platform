package uk.ac.wellcome.platform.api.models

import scala.util.{Failure, Success}
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty}
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.get.GetResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models._
import uk.ac.wellcome.platform.api.utils.ApiJsonUtil

@ApiModel(
  value = "Work",
  description =
    "An individual work such as a text, archive item or picture; or a grouping of individual works (so, for instance, an archive collection counts as a work, as do all the series and individual files within it).  Each work may exist in multiple instances (e.g. copies of the same book).  N.B. this is not synonymous with \\\"work\\\" as that is understood in the International Federation of Library Associations and Institutions' Functional Requirements for Bibliographic Records model (FRBR) but represents something lower down the FRBR hierarchy, namely manifestation. Groups of related items are also included as works because they have similar properties to the individual ones."
)
case class DisplayWork(
  @ApiModelProperty(
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: String,
  @ApiModelProperty(value =
    "The title or other short label of a work, including labels not present in the actual work or item but applied by the cataloguer for the purposes of search or description.") title: String,
  @ApiModelProperty(
    dataType = "String",
    value = "A description given to a thing.") description: Option[String] =
    None,
  @ApiModelProperty(
    dataType = "String",
    value = "Recording written text on a (usually visual) work.") lettering: Option[
    String] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.models.Period",
    value =
      "Relates the creation of a work to a date, when the date of creation does not cover a range.") createdDate: Option[
    Period] = None,
  @ApiModelProperty(value =
    "Relates a work to its author, compiler, editor, artist or other entity responsible for its coming into existence in the form that it has.") creators: List[
    Agent] = List(),
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
  @ApiModelProperty(value =
    "Relates a work to the general thesaurus-based concept that describes the work's content.") subjects: List[
    Concept] = List(),
  @ApiModelProperty(value =
    "Relates a work to the genre that describes the work's content.") genres: List[
    Concept] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.platform.api.models.DisplayLocation",
    value =
      "Relates any thing to the location of a representative thumbnail image"
  ) thumbnail: Option[DisplayLocation] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayItem]",
    value = "List of items related to this work."
  ) items: Option[List[DisplayItem]] = None,
  @JsonIgnore visible: Boolean = true
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWork {

  def apply(work: Work, includes: WorksIncludes): DisplayWork = {
    DisplayWork(
      id = work.id,
      title = work.title,
      description = work.description,
      lettering = work.lettering,
      createdDate = work.createdDate,
      // Wrapping this in Option to catch null value from Jackson
      creators = Option(work.creators).getOrElse(Nil),
      subjects = Option(work.subjects).getOrElse(Nil),
      genres = Option(work.genres).getOrElse(Nil),
      identifiers =
        if (includes.identifiers)
          // If there aren't any identifiers on the work JSON, Jackson puts a
          // nil here.  Wrapping it in an Option casts it into a None or Some
          // as appropriate, and avoids throwing a NullPointerError when
          // we map over the value.
          Option[List[SourceIdentifier]](work.identifiers) match {
            case Some(identifiers) =>
              Some(identifiers.map(DisplayIdentifier(_)))
            case None => Some(List())
          } else None,
      thumbnail =
        if (includes.thumbnail)
          work.thumbnail.map(DisplayLocation(_))
        else None,
      items =
        if (includes.items)
          Option[List[Item]](work.items) match {
            case Some(items) =>
              Some(items.map(DisplayItem(_, includes.identifiers)))
            case None => Some(List())
          } else None,
      visible = work.visible
    )
  }

  def apply(hit: SearchHit): DisplayWork =
    apply(hit, includes = WorksIncludes())

  def apply(hit: SearchHit, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(hit.sourceAsString, includes)
  }

  def apply(got: GetResponse, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(got.sourceAsString, includes)
  }

  private def jsonToDisplayWork(document: String, includes: WorksIncludes) = {
    ApiJsonUtil.fromJson[Work](document) match {
      case Success(work) => DisplayWork(work = work, includes = includes)
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON as Work ($e): $document"
        )
    }
  }
}
