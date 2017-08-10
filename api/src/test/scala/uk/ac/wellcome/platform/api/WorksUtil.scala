package uk.ac.wellcome.platform.api

import uk.ac.wellcome.finatra.modules.IdentifierSchemes
import uk.ac.wellcome.models._
import uk.ac.wellcome.platform.api.models.DisplayWork

trait WorksUtil {

  val canonicalId = "1234"
  val title = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"

  val period = Period("the past")
  val agent = Agent("a person")

  def convertWorkToDisplayWork(work: IdentifiedWork) = DisplayWork(
    work.canonicalId,
    work.work.title,
    work.work.description,
    work.work.lettering,
    work.work.createdDate,
    work.work.creators
  )

  def createIdentifiedWorks(count: Int): Seq[IdentifiedWork] =
    (1 to count).map(
      (idx: Int) =>
        identifiedWorkWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}")
      ))

  def identifiedWorkWith(canonicalId: String, title: String): IdentifiedWork =
    IdentifiedWork(canonicalId,
                   Work(identifiers =
                          List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
                        title = title))

  def identifiedWorkWith(canonicalId: String,
                         title: String,
                         identifiers: List[SourceIdentifier]): IdentifiedWork =
    IdentifiedWork(canonicalId, Work(identifiers = identifiers, title = title))

  def identifiedWorkWith(canonicalId: String,
                         title: String,
                         description: String,
                         lettering: String,
                         createdDate: Period,
                         creator: Agent): IdentifiedWork = IdentifiedWork(
    canonicalId = canonicalId,
    work = Work(
      identifiers = List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
      title = title,
      description = Some(description),
      lettering = Some(lettering),
      createdDate = Some(createdDate),
      creators = List(creator)
    )
  )
}
