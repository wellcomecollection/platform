package uk.ac.wellcome.test.utils

import uk.ac.wellcome.models._

trait WorksUtil {
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
