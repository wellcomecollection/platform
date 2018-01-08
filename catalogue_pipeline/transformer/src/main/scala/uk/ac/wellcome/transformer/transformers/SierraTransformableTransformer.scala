package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.MergedSierraRecord
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Success, Try}

case class SierraBibData(id: String, title: String)

class SierraTransformableTransformer
    extends TransformableTransformer[MergedSierraRecord] {
  override def transformForType(
    sierraTransformable: MergedSierraRecord): Try[Option[Work]] =
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
