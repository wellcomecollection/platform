package uk.ac.wellcome.platform.transformer.transformers

import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  CalmTransformableData
}
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  SourceIdentifier,
  UnidentifiedWork
}
import uk.ac.wellcome.utils.JsonUtil._

class CalmTransformableTransformer
    extends TransformableTransformer[CalmTransformable] {
  override def transformForType = {
    case (calmTransformable: CalmTransformable, version: Int) =>
      fromJson[CalmTransformableData](calmTransformable.data)
        .map(
          _ =>
            Some(
              UnidentifiedWork(
                title = "placeholder title",
                sourceIdentifier = SourceIdentifier(
                  identifierType = IdentifierType("calm-altref-no"),
                  ontologyType = "Work",
                  value = "value"
                ),
                version = version
              )))
  }
}
