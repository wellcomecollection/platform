package uk.ac.wellcome.platform.api.models

import scala.util.{Failure, Success}
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import com.sksamuel.elastic4s.http.search.SearchHit
import com.sksamuel.elastic4s.http.get.GetResponse
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{Item, SourceIdentifier, Work}
import uk.ac.wellcome.utils.JsonUtil._

@JsonIgnoreProperties(Array("visible"))
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
    dataType = "uk.ac.wellcome.platform.api.models.DisplayPeriod",
    value =
      "Relates the creation of a work to a date, when the date of creation does not cover a range.") createdDate: Option[
    DisplayPeriod] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayAgent]",
    value =
      "Relates a work to its author, compiler, editor, artist or other entity responsible for its coming into existence in the form that it has.") creators: List[
    DisplayAgent] = List(),
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.platform.api.models.DisplayConcept",
    value =
      "Relates a work to the general thesaurus-based concept that describes the work's content.") subjects: List[
    DisplayConcept] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.platform.api.models.DisplayConcept",
    value =
      "Relates a work to the genre that describes the work's content.") genres: List[
    DisplayConcept] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.platform.api.models.DisplayLocation",
    value =
      "Relates any thing to the location of a representative thumbnail image"
  ) thumbnail: Option[DisplayLocation] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayItem]",
    value = "List of items related to this work."
  ) items: Option[List[DisplayItem]] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayAgent]",
    value = "Relates a published work to its publisher."
  ) publishers: List[DisplayAgent] = List(),
  visible: Boolean = true
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWork {

  def apply(work: Work, includes: WorksIncludes): DisplayWork = {
    DisplayWork(
      id = work.id,
      title = work.title.get,
      description = work.description,
      lettering = work.lettering,
      createdDate = work.createdDate.map { DisplayPeriod(_) },
      creators = work.creators.map { DisplayAgent(_) },
      subjects = work.subjects.map { DisplayConcept(_) },
      genres = work.genres.map { DisplayConcept(_) },
      identifiers =
        if (includes.identifiers)
          work.identifiers match {
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
          work.items match {
            case Some(items) =>
              Some(items.map(DisplayItem(_, includes.identifiers)))
            case None => Some(List())
          } else None,
      publishers = work.publishers.map(DisplayAgent(_)),
      visible = work.visible
    )
  }

  def apply(work: Work): DisplayWork =
    DisplayWork(work = work, includes = WorksIncludes())

  def apply(hit: SearchHit): DisplayWork =
    apply(hit, includes = WorksIncludes())

  def apply(hit: SearchHit, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(hit.sourceAsString, includes)
  }

  def apply(got: GetResponse, includes: WorksIncludes): DisplayWork = {
    jsonToDisplayWork(got.sourceAsString, includes)
  }

  private def jsonToDisplayWork(document: String, includes: WorksIncludes) = {
    fromJson[Work](document) match {
      case Success(work) => DisplayWork(work = work, includes = includes)
      case Failure(e) =>
        throw new RuntimeException(
          s"Unable to parse JSON as Work ($e): $document"
        )
    }
  }
}
