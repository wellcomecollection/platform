package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

trait WorksUtil {
  val canonicalId = "1234"
  val title = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"
  val period = Period("the past")
  val agent = Agent("a person")
  val workType = WorkType(
    id = "1dz4yn34va",
    label = "An aggregation of angry archipelago aged ankylosaurs."
  )
  val subject = Subject(
    "a subject",
    List(
      Concept("a subject concept"),
      Place("a subject place"),
      Period("a subject period")))
  val genre = Genre(
    "a genre",
    List(
      Concept("a genre concept"),
      Place("a genre place"),
      Period("a genre period")))
  val sourceIdentifier = SourceIdentifier(
    IdentifierSchemes.miroImageNumber,
    "Work",
    "sourceIdentifierFromWorksUtil"
  )

  def createWork(visible: Boolean = true, version: Int = 1): IdentifiedWork =
    createWorks(count = 1, visible = visible, version = version).head

  def createWorks(count: Int,
                  start: Int = 1,
                  visible: Boolean = true,
                  version: Int = 1): Seq[IdentifiedWork] =
    (start to count).map(
      (idx: Int) =>
        workWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}"),
          items = List(defaultItem),
          visible = visible,
          version = version
      ))

  def workWith(canonicalId: String, title: String): IdentifiedWork =
    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = canonicalId)

  def workWith(canonicalId: String,
               title: String,
               visible: Boolean): IdentifiedWork =
    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = canonicalId,
      visible = visible)

  def workWith(
    canonicalId: String,
    title: String,
    identifiers: List[SourceIdentifier] = List(),
    items: List[IdentifiedItem] = List()
  ): IdentifiedWork =
    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = identifiers,
      canonicalId = canonicalId,
      items = items)

  def identifiedWorkWith(
    canonicalId: String,
    title: String,
    thumbnail: Location
  ): IdentifiedWork =
    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(
        SourceIdentifier(IdentifierSchemes.miroImageNumber, "Work", "5678")
      ),
      canonicalId = canonicalId,
      thumbnail = Some(thumbnail)
    )

  def workWith(canonicalId: String,
               title: String,
               description: String,
               lettering: String,
               createdDate: Period,
               creator: Agent,
               items: List[IdentifiedItem],
               visible: Boolean): IdentifiedWork =
    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(sourceIdentifier),
      canonicalId = canonicalId,
      workType = Some(workType),
      description = Some(description),
      lettering = Some(lettering),
      createdDate = Some(createdDate),
      contributors = List(
        Contributor(agent = Unidentifiable(creator))
      ),
      items = items,
      visible = visible
    )

  def workWith(canonicalId: String,
               title: String,
               description: String,
               lettering: String,
               createdDate: Period,
               creator: Agent,
               subjects: List[Subject],
               genres: List[Genre],
               items: List[IdentifiedItem],
               visible: Boolean,
               version: Int): IdentifiedWork =
    IdentifiedWork(
      title = Some(title),
      sourceIdentifier = sourceIdentifier,
      version = version,
      identifiers = List(sourceIdentifier),
      canonicalId = canonicalId,
      workType = Some(workType),
      description = Some(description),
      lettering = Some(lettering),
      createdDate = Some(createdDate),
      contributors = List(
        Contributor(agent = Unidentifiable(creator))
      ),
      subjects = subjects,
      genres = genres,
      items = items,
      visible = visible
    )

  def defaultItem: IdentifiedItem = {
    itemWith(
      "item-canonical-id",
      defaultItemSourceIdentifier,
      defaultLocation
    )
  }

  def defaultItemSourceIdentifier = {
    SourceIdentifier(IdentifierSchemes.miroImageNumber, "Item", "M0000001")
  }

  def defaultLocation: Location = {
    digitalLocationWith(
      "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json",
      License_CCBY)
  }

  def itemWith(
    canonicalId: String,
    identifier: SourceIdentifier,
    location: Location
  ): IdentifiedItem = IdentifiedItem(
    canonicalId = canonicalId,
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  def digitalLocationWith(url: String, license: License): DigitalLocation = {
    DigitalLocation(locationType = "iiif-image", url = url, license = license)
  }
}
