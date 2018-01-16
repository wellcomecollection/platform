package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.transformable.{
  CalmTransformable,
  CalmTransformableData,
  Transformable
}
import uk.ac.wellcome.models._
import uk.ac.wellcome.circe.jsonUtil._

import scala.util.Try

class CalmTransformableTransformer
    extends TransformableTransformer[CalmTransformable] {
  override def transformForType(
    calmTransformable: CalmTransformable): Try[Option[Work]] =
    fromJson[CalmTransformableData](calmTransformable.data)
      .flatMap(data => (new CalmDataTransformer).transform(data))
}

class CalmDataTransformer
    extends TransformableTransformer[CalmTransformableData] {
  override def transformForType(
    transformable: CalmTransformableData): Try[Option[Work]] = Try {
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
