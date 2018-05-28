package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.models.work.internal.IdentifiedWork

@ApiModel(
  value = "Work",
  description =
    "An individual work such as a text, archive item or picture; or a grouping of individual works (so, for instance, an archive collection counts as a work, as do all the series and individual files within it).  Each work may exist in multiple instances (e.g. copies of the same book).  N.B. this is not synonymous with \\\"work\\\" as that is understood in the International Federation of Library Associations and Institutions' Functional Requirements for Bibliographic Records model (FRBR) but represents something lower down the FRBR hierarchy, namely manifestation. Groups of related items are also included as works because they have similar properties to the individual ones."
)
case class DisplayWorkV2(
  @ApiModelProperty(
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: String,
  @ApiModelProperty(
    value =
      "The title or other short label of a work, including labels not present in the actual work or item but applied by the cataloguer for the purposes of search or description."
  ) title: String,
  @ApiModelProperty(
    dataType = "String",
    value = "A description given to a thing."
  ) description: Option[String] = None,
  @ApiModelProperty(
    dataType = "String",
    value = "A description of specific physical characteristics of the work."
  ) physicalDescription: Option[String] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayWorkType",
    value = "The type of work."
  ) workType: Option[DisplayWorkType] = None,
  @ApiModelProperty(
    dataType = "String",
    value =
      "Number of physical pages, volumes, cassettes, total playing time, etc., of of each type of unit"
  ) extent: Option[String] = None,
  @ApiModelProperty(
    dataType = "String",
    value = "Recording written text on a (usually visual) work."
  ) lettering: Option[String] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.v2.DisplayPeriod",
    value =
      "Relates the creation of a work to a date, when the date of creation does not cover a range."
  ) createdDate: Option[DisplayPeriod] = None,
  @ApiModelProperty(
    value =
      "Relates a work to its author, compiler, editor, artist or other entity responsible for its coming into existence in the form that it has."
  ) contributors: List[DisplayContributor] = List(),
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]] = None,
  @ApiModelProperty(
    value =
      "Relates a work to the general thesaurus-based concept that describes the work's content."
  ) subjects: List[DisplaySubject] = List(),
  @ApiModelProperty(
    value = "Relates a work to the genre that describes the work's content."
  ) genres: List[DisplayGenre] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayLocation",
    value =
      "Relates any thing to the location of a representative thumbnail image"
  ) thumbnail: Option[DisplayLocation] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayItemV2]",
    value = "List of items related to this work."
  ) items: Option[List[DisplayItemV2]] = None,
  @ApiModelProperty(
    value = "Relates a published work to its publisher."
  ) publishers: List[DisplayAbstractAgentV2] = List(),
  @ApiModelProperty(
    value = "Show a list of places of publication."
  ) placesOfPublication: List[DisplayPlace] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.v2.DisplayPeriod",
    value =
      "Relates the publication of a work to a date when the work has been formally published."
  ) publicationDate: Option[DisplayPeriod] = None,
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayLanguage",
    value = "Relates a work to its primary language."
  ) language: Option[DisplayLanguage] = None,
  @ApiModelProperty(
    dataType = "String"
  ) dimensions: Option[String] = None
) extends DisplayWork {
  @ApiModelProperty(
    readOnly = true,
    value =
      "A broad, top-level description of the form of a work: namely, whether it is a printed book, archive, painting, photograph, moving image, etc."
  )
  @JsonProperty("type") val ontologyType: String = "Work"
}

case object DisplayWorkV2 {

  def apply(work: IdentifiedWork, includes: WorksIncludes): DisplayWorkV2 = {

    if (!work.visible) {
      throw new RuntimeException(
        s"IdentifiedWork ${work.canonicalId} has visible=false, cannot be converted to DisplayWork")
    }

    if (work.title.isEmpty && work.visible) {
      throw new RuntimeException(
        s"IdentifiedWork ${work.canonicalId} has visible=true, but no title! cannot be converted to DisplayWork")
    }

    DisplayWorkV2(
      id = work.canonicalId,
      title = work.title.get,
      description = work.description,
      physicalDescription = work.physicalDescription,
      extent = work.extent,
      lettering = work.lettering,
      createdDate = work.createdDate.map { DisplayPeriod(_) },
      contributors = work.contributors.map {
        DisplayContributor(_, includesIdentifiers = includes.identifiers)
      },
      subjects = work.subjects.map {
        DisplaySubject(_, includesIdentifiers = includes.identifiers)
      },
      genres = work.genres.map {
        DisplayGenre(_, includesIdentifiers = includes.identifiers)
      },
      identifiers =
        if (includes.identifiers)
          Some(work.identifiers.map { DisplayIdentifierV2(_) })
        else None,
      workType = work.workType.map { DisplayWorkType(_) },
      thumbnail =
        if (includes.thumbnail)
          work.thumbnail.map { DisplayLocation(_) } else None,
      items =
        if (includes.items)
          Some(work.items.map {
            DisplayItemV2(_, includesIdentifiers = includes.identifiers)
          })
        else None,
      publishers = work.publishers.map {
        DisplayAbstractAgentV2(_, includesIdentifiers = includes.identifiers)
      },
      publicationDate = work.publicationDate.map { DisplayPeriod(_) },
      placesOfPublication = work.placesOfPublication.map { DisplayPlace(_) },
      language = work.language.map { DisplayLanguage(_) },
      dimensions = work.dimensions
    )
  }

  def apply(work: IdentifiedWork): DisplayWorkV2 =
    DisplayWorkV2(work = work, includes = WorksIncludes())
}
