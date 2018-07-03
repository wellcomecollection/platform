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
                sourceIdentifier = SourceIdentifier(
                  identifierType = IdentifierType("calm-altref-no"),
                  ontologyType = "Work",
                  value = "value"
                ),
                otherIdentifiers = List(),
                mergeCandidates = List(),
                title = "placeholder title",
                workType = None,
                description = None,
                physicalDescription = None,
                extent = None,
                lettering = None,
                createdDate = None,
                subjects = List(),
                genres = List(),
                contributors = List(),
                thumbnail = None,
                production = List(),
                language = None,
                dimensions = None,
                items = List(),
                version = version
              )
          )
        )
  }
}
