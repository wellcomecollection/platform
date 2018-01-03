package uk.ac.wellcome.transformer.transformers

import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibTransformableData,
  SierraItemTransformableData
}
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Success, Try}

class SierraTransformableTransformer
    extends TransformableTransformer[MergedSierraRecord] {
  override def transformForType(
    sierraTransformable: MergedSierraRecord): Try[Option[Work]] =
    sierraTransformable.maybeBibData
      .map { bibRecord =>
        val bibData = SierraBibTransformableData.create(bibRecord.data)
        JsonUtil.fromJson[SierraBibData](bibRecord.data).map { sierraBibData =>
          Some(Work(
            title = getTitle(bibData),
            publishers = getPublishers(bibData),

            // TODO: Rewrite this to use the transformableData
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
      // A merged record can have both bibs and items.  If we only have
      // the item data so far, we don't have enough to build a Work, so we
      // return None.
      .getOrElse(Success(None))

  // Populate wwork:title.  The rules are as follows:
  //
  //    For all bibliographic records use Sierra "title".
  //
  // Note: Sierra populates this field from MARC field 245 subfields $a and $b.
  // http://www.loc.gov/marc/bibliographic/bd245.html
  private def getTitle(bibData: SierraBibTransformableData): String =
    bibData.title.get

  // Populate wwork:publishers.
  //
  //    For bibliographic records where "260" is populated:
  //    - "label" comes from MARC field 260 subfield $b.
  //    - "type" is "Organisation"
  //
  // Note that subfield $b can occur more than once on a record.
  //
  // http://www.loc.gov/marc/bibliographic/bd260.html
  private def getPublishers(bibData: SierraBibTransformableData): List[Agent] = {
    val matchingSubfields = bibData.varFields
      .filter { _.marcTag == Some("260") }
      .flatMap { _.subfields }
      .flatten

    matchingSubfields
      .filter { _.tag == "b" }
      .map { subfield => Agent(
        label = subfield.content,
        ontologyType = "Organisation"
      )}
    }
}
