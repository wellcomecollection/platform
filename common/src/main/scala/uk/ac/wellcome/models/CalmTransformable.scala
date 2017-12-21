package uk.ac.wellcome.models

import uk.ac.wellcome.models.transformable._
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

case class CalmTransformableData(
  AccessStatus: Array[String]
) {
  def transform: Try[Option[Work]] = Try {
    // TODO: Fill in proper data here
    Some(
      Work(
        sourceIdentifier = SourceIdentifier(
          IdentifierSchemes.calmPlaceholder,
          "value"
        ),
        title = "placeholder title for a Calm record",
        identifiers = List(
          SourceIdentifier(
            IdentifierSchemes.calmPlaceholder,
            "value"
          ))
      ))
  }
}
