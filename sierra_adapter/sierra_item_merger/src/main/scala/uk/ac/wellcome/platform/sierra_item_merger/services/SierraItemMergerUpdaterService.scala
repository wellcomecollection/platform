package uk.ac.wellcome.platform.sierra_item_merger.services

import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemLinker
import uk.ac.wellcome.platform.sierra_item_merger.links.ItemUnlinker
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.VersionedHybridStore
import uk.ac.wellcome.models.{SourceMetadata, Sourced}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class SierraItemMergerUpdaterService @Inject()(
  versionedHybridStore: VersionedHybridStore[SierraTransformable],
  metrics: MetricsSender
) extends Logging {

  val sourceName = "sierra"

  def update(itemRecord: SierraItemRecord): Future[Unit] = {

    implicit val decoder = Decoder[SierraTransformable]
    implicit val encoder = Encoder[SierraTransformable]

    val mergeUpdateFutures = itemRecord.bibIds.map { bibId =>
      versionedHybridStore.updateRecord(Sourced.id(sourceName, bibId))(
        ifNotExisting = SierraTransformable(
          sourceId = bibId,
          itemData = Map(itemRecord.id -> itemRecord)
        ))(ifExisting = existingSierraTransformable => {
        ItemLinker.linkItemRecord(
          existingSierraTransformable,
          itemRecord
        )
      })(SourceMetadata(sourceName))
    }

    val unlinkUpdateFutures: Seq[Future[Unit]] =
      itemRecord.unlinkedBibIds.map { unlinkedBibId =>
        versionedHybridStore.updateRecord(
          Sourced.id(sourceName, unlinkedBibId))(
          throw GracefulFailureException(
            new RuntimeException(
              s"Missing Bib record to unlink: $unlinkedBibId")
          )
        )((record: SierraTransformable) =>
          ItemUnlinker.unlinkItemRecord(record, itemRecord))(
          SourceMetadata(sourceName))
      }

    Future.sequence(mergeUpdateFutures ++ unlinkUpdateFutures).map(_ => ())
  }

}
