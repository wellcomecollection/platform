package uk.ac.wellcome.platform.sierra_item_merger.fixtures

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.platform.sierra_item_merger.services.SierraItemMergerUpdaterService
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith

trait SierraItemMergerFixtures {
  def withSierraUpdaterService[R](
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      EmptyMetadata,
                                      ObjectStore[SierraTransformable]])(
    testWith: TestWith[SierraItemMergerUpdaterService, R]): R = {
    val sierraUpdaterService = new SierraItemMergerUpdaterService(
      versionedHybridStore = hybridStore
    )
    testWith(sierraUpdaterService)
  }
}
