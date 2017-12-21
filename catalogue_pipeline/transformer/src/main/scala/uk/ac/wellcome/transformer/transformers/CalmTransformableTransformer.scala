package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models._
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

class CalmTransformableTransformer extends TransformableTransformer[CalmTransformable] {
  override def transform(transformable: Transformable): Try[Option[Work]] = transformable match {
    case calmTransformable: CalmTransformable => JsonUtil
      .fromJson[CalmTransformableData](calmTransformable.data)
      .flatMap(data => (new CalmDataTransformer).transform(data))
    case _ => throw new RuntimeException
  }
}

class CalmDataTransformer extends TransformableTransformer[CalmTransformableData] {
  override def transform(transformable: Transformable): Try[Option[Work]] = Try {
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
