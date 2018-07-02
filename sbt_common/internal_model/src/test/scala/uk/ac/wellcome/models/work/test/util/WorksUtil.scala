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
  val subject = Subject[Unidentifiable[AbstractConcept]](
    label = "a subject created by WorksUtil",
    concepts = List(
      Unidentifiable(Concept("a subject concept")),
      Unidentifiable(Place("a subject place")),
      Unidentifiable(Period("a subject period")))
  )

  val genre = Genre[Unidentifiable[AbstractConcept]](
    label = "an unidentified genre created by WorksUtil",
    concepts = List(
      Unidentifiable(Concept("a genre concept")),
      Unidentifiable(Place("a genre place")),
      Unidentifiable(Period("a genre period")))
  )

  val sourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    "Work",
    "sourceIdentifierFromWorksUtil"
  )

  def createWork: IdentifiedWork =
    createWorks(count = 1).head

  def createWorks(count: Int, start: Int = 1): Seq[IdentifiedWork] =
    (start to count).map(
      (idx: Int) =>
        workWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}"),
          items = List(defaultItem)
      ))

  def createInvisibleWorks(count: Int,
                           start: Int = 1): Seq[IdentifiedInvisibleWork] =
    (start to count).map(
      (idx: Int) => invisibleWorkWith(s"$idx-$canonicalId")
    )

  def invisibleWorkWith(canonicalId: String): IdentifiedInvisibleWork = {
    IdentifiedInvisibleWork(
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId
    )
  }

  def workWith(canonicalId: String, title: String): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId)

  def workWith(
    canonicalId: String,
    title: String,
    otherIdentifiers: List[SourceIdentifier] = List(),
    items: List[Identified[Item]] = List()
  ): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      otherIdentifiers = otherIdentifiers,
      canonicalId = canonicalId,
      items = items)

  def identifiedWorkWith(
    canonicalId: String,
    title: String,
    thumbnail: Location
  ): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId,
      thumbnail = Some(thumbnail)
    )

  def workWith(canonicalId: String,
               title: String,
               description: String,
               lettering: String,
               createdDate: Period,
               creator: Agent,
               items: List[Identified[Item]]): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId,
      workType = Some(workType),
      description = Some(description),
      lettering = Some(lettering),
      createdDate = Some(createdDate),
      contributors = List(
        Contributor(agent = Unidentifiable(creator))
      ),
      production = List(),
      items = items
    )

  def workWith(canonicalId: String,
               title: String,
               description: String,
               lettering: String,
               createdDate: Period,
               creator: Agent,
               subjects: List[Subject[Displayable[AbstractConcept]]],
               genres: List[Genre[Displayable[AbstractConcept]]],
               items: List[Identified[Item]]): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
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
      items = items
    )

  def defaultItem: Identified[Item] = itemWith(
    "item-canonical-id",
    defaultItemSourceIdentifier,
    defaultLocation
  )

  def defaultItemSourceIdentifier = {
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      "Item",
      "M0000001")
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
  ): Identified[Item] =
    Identified(
      canonicalId = canonicalId,
      sourceIdentifier = identifier,
      agent = Item(
        locations = List(location)
      ))

  def digitalLocationWith(url: String, license: License): DigitalLocation = {
    DigitalLocation(
      locationType = LocationType("iiif-image"),
      url = url,
      license = license)
  }
}
