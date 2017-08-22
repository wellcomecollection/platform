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

  def convertWorkToDisplayWork(work: Work) = DisplayWork(
    work.id,
    work.title,
    work.description,
    work.lettering,
    work.createdDate,
    work.creators
  )

  def createWorks(count: Int): Seq[Work] =
    (1 to count).map(
      (idx: Int) =>
        workWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}")
      ))

  def workWith(canonicalId: String, title: String): Work =
    Work(Some(canonicalId),
                   identifiers =
                          List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
                        title = title)

  def workWith(canonicalId: String,
               title: String,
               identifiers: List[SourceIdentifier]): Work =
    Work(Some(canonicalId), identifiers = identifiers, title = title)

  def identifiedWorkWith(canonicalId: String,
                         title: String,
                         thumbnail: Location): Work =
    Work(
      Some(canonicalId),
      identifiers = List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
      title = title,
      thumbnail = Some(thumbnail)
    )

  def workWith(canonicalId: String,
                         title: String,
                         description: String,
                         lettering: String,
                         createdDate: Period,
                         creator: Agent): Work = Work(
    canonicalId = Some(canonicalId),
      identifiers = List(SourceIdentifier(IdentifierSchemes.miroImageNumber, "5678")),
      title = title,
      description = Some(description),
      lettering = Some(lettering),
      createdDate = Some(createdDate),
      creators = List(creator)

  )
}
