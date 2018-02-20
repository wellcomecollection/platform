package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  CalmTransformableData,
  Transformable
}
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Try

class CalmTransformableTransformer
    extends TransformableTransformer[CalmTransformable] {
  override def transformForType(
    calmTransformable: CalmTransformable, version: Int): Try[Option[Work]] =
    fromJson[CalmTransformableData](calmTransformable.data)
      .map(
        _ =>
          Some(
            Work(
              title = Some("placeholder title for a Calm record"),
              sourceIdentifier = SourceIdentifier(
                IdentifierSchemes.calmPlaceholder,
                "value"
              ),
              version = version,
              identifiers = List(
                SourceIdentifier(
                  IdentifierSchemes.calmPlaceholder,
                  "value"
                ))
            )))
}
