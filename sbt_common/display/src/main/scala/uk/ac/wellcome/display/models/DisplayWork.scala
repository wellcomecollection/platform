package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonProperty}
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}

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
    value = "A description of specific physical characteristics of the work.") physicalDescription: Option[
    String] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayWorkType]",
    value = "The type of work.") workType: Option[DisplayWorkType] = None,
  @ApiModelProperty(
    dataType = "String",
    value =
      "Number of physical pages, volumes, cassettes, total playing time, etc., of of each type of unit"
  ) extent: Option[String] = None,
  @ApiModelProperty(
    dataType = "String",
    value = "Recording written text on a (usually visual) work.") lettering: Option[
    String] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayPeriod",
    value =
      "Relates the creation of a work to a date, when the date of creation does not cover a range."
  ) createdDate: Option[DisplayPeriod] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayAgent]",
    value =
      "Relates a work to its author, compiler, editor, artist or other entity responsible for its coming into existence in the form that it has."
  ) creators: List[DisplayAgent] = List(),
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayConcept",
    value =
      "Relates a work to the general thesaurus-based concept that describes the work's content."
  ) subjects: List[DisplayConcept] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayConcept",
    value = "Relates a work to the genre that describes the work's content.") genres: List[
    DisplayConcept] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayLocation",
    value =
      "Relates any thing to the location of a representative thumbnail image"
  ) thumbnail: Option[DisplayLocation] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayItem]",
    value = "List of items related to this work."
  ) items: Option[List[DisplayItem]] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayAgent]",
    value = "Relates a published work to its publisher."
  ) publishers: List[DisplayAgent] = List(),
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayPlace]",
    value = "Show a list of places of publication."
  ) placesOfPublication: List[DisplayPlace] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayPeriod",
    value =
      "Relates the publication of a work to a date when the work has been formally published."
  ) publicationDate: Option[DisplayPeriod] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayLanguage",
    value = "Relates a work to its primary language."
  ) language: Option[DisplayLanguage] = None,
  visible: Boolean = true
) {
  @ApiModelProperty(
    readOnly = true,
    value =
      "A broad, top-level description of the form of a work: namely, whether it is a printed book, archive, painting, photograph, moving image, etc."
  )
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWork {

  def apply(work: IdentifiedWork, includes: WorksIncludes): DisplayWork = {
    DisplayWork(
      id = work.canonicalId,
      title = work.title.get,
      description = work.description,
      physicalDescription = work.physicalDescription,
      extent = work.extent,
      lettering = work.lettering,
      createdDate = work.createdDate.map { DisplayPeriod(_) },
      creators = work.creators.map { DisplayAgent(_) },
      subjects = work.subjects.map { DisplayConcept(_) },
      genres = work.genres.map { DisplayConcept(_) },
      identifiers =
        if (includes.identifiers)
          Some(work.identifiers.map { DisplayIdentifier(_) })
        else None,
      workType = work.workType.map { DisplayWorkType(_) },
      thumbnail =
        if (includes.thumbnail)
          work.thumbnail.map { DisplayLocation(_) } else None,
      items =
        if (includes.items)
          Some(work.items.map {
            DisplayItem(_, includesIdentifiers = includes.identifiers)
          })
        else None,
      publishers = work.publishers.map(DisplayAgent(_)),
      publicationDate = work.publicationDate.map { DisplayPeriod(_) },
      placesOfPublication = work.placesOfPublication.map { DisplayPlace(_) },
      language = work.language.map { DisplayLanguage(_) },
      visible = work.visible
    )
  }

  def apply(work: IdentifiedWork): DisplayWork =
    DisplayWork(work = work, includes = WorksIncludes())
}
