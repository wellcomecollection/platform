package uk.ac.wellcome.platform.api

import uk.ac.wellcome.models._
import uk.ac.wellcome.platform.api.models.DisplayWork

trait WorksUtil {

  val canonicalId = "1234"
  val label = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"

  val period = Period("the past")
  val agent = Agent("a person")


  def convertWorkToDisplayWork(work: IdentifiedWork) = DisplayWork(
    "Work",
    work.canonicalId,
    work.work.label,
    work.work.description,
    work.work.lettering,
    work.work.hasCreatedDate,
    work.work.hasCreator
  )

  def createIdentifiedWorks(count: Int) = (1 to count).map(
    (idx: Int) =>
      identifiedWorkWith(
        canonicalId = s"${idx}-${canonicalId}",
        label = s"${idx}-${label}",
        description = s"${idx}-${description}",
        lettering = s"${idx}-${lettering}",
        createdDate = period.copy(label = s"${idx}-${period.label}"),
        creator = agent.copy(label = s"${idx}-${agent.label}")
      ))

  def identifiedWorkWith(canonicalId: String, label: String): IdentifiedWork = {
    IdentifiedWork(canonicalId,
                   Work(identifiers =
                          List(SourceIdentifier("Miro", "MiroID", "5678")),
                        label = label))

  }
  def identifiedWorkWith(canonicalId: String,
                         label: String,
                         description: String,
                         lettering: String,
                         createdDate: Period,
                         creator: Agent): IdentifiedWork = {

    IdentifiedWork(
      canonicalId = canonicalId,
      work = Work(
        identifiers = List(SourceIdentifier("Miro", "MiroID", "5678")),
        label = label,
        description = Some(description),
        lettering = Some(lettering),
        hasCreatedDate = Some(createdDate),
        hasCreator = List(creator)
      )
    )
  }
}
