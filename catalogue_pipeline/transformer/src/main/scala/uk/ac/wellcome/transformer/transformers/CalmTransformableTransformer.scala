package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  CalmTransformableData
}
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Try

class CalmTransformableTransformer
    extends TransformableTransformer[CalmTransformable] {
  override def transformForType(calmTransformable: CalmTransformable,
                                version: Int): Try[Option[UnidentifiedWork]] =
    fromJson[CalmTransformableData](calmTransformable.data)
      .map(
        _ =>
          Some(
            UnidentifiedWork(
              title = Some("placeholder title"),
              sourceIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.calmPlaceholder,
                ontologyType = "Work",
                value = "value"
              ),
              version = version,
              identifiers = List(
                SourceIdentifier(
                  identifierScheme = IdentifierSchemes.calmPlaceholder,
                  ontologyType = "Work",
                  value = "value"
                ))
            )))
}
