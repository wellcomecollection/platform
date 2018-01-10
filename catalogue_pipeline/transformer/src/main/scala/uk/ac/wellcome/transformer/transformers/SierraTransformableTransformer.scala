package uk.ac.wellcome.transformer.transformers

import com.twitter.inject.Logging
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Success, Try}

case class SierraBibData(id: String, title: String)

class SierraTransformableTransformer
    extends TransformableTransformer[SierraTransformable]
    with Logging {
  override def transformForType(
    sierraTransformable: SierraTransformable): Try[Option[Work]] = {
    sierraTransformable.maybeBibData
      .map { bibData =>
        info(s"Attempting to transform $bibData")

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
            ),
            items = Option(sierraTransformable.itemData)
              .getOrElse(Map.empty)
              .values
              .map(record =>
                Item(
                  sourceIdentifier = SourceIdentifier(
                    IdentifierSchemes.sierraSystemNumber,
                    record.id
                  ),
                  identifiers = List(
                    SourceIdentifier(
                      IdentifierSchemes.sierraSystemNumber,
                      record.id
                    )
                  )
              ))
              .toList
          ))
        }
      }
      .getOrElse(Success(None))
  }

}
