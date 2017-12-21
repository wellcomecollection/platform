package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Success, Try}

class SierraTransformableTransformer extends TransformableTransformer[MergedSierraRecord] {
  override def transform(transformable: Transformable): Try[Option[Work]] =
    transformable match {
      case sierraTransformable: MergedSierraRecord =>
      sierraTransformable.maybeBibData
        .map { bibData =>
          JsonUtil.fromJson[SierraBibData](bibData.data).map { sierraBibData =>
            Some(Work(
              title = sierraBibData.title,
              sourceIdentifier = SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              ),
              identifiers = List(
                SourceIdentifier(
                  identifierScheme = IdentifierSchemes.sierraSystemNumber,
                  sierraBibData.id
                )
              )
            ))
          }
        }
        .getOrElse(Success(None))
      case _ => throw new RuntimeException
    }

}
