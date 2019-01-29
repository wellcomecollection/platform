package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectListing
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.bagreplicator.models.StorageLocation
import uk.ac.wellcome.platform.archive.common.models.BagLocation

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object BagStorage extends Logging {

  def duplicateBag(sourceBagLocation: BagLocation,
                   storageDestination: StorageLocation)(
    implicit s3Client: AmazonS3,
    s3Copier: S3Copier,
    executionContext: ExecutionContext) = Future {
    debug(s"duplicating bag from $sourceBagLocation to $storageDestination")
    val sourceBagItems = listBagItems(sourceBagLocation)
    duplicateBagItems(sourceBagItems, storageDestination)
  }

  private def listObjects(s3Client: AmazonS3, bagLocation: BagLocation) = {
    // We must terminate the prefix with "/" to search only under that
    // "folder" and not other keys that match that prefix.
    // e.g. prefix "foo" matches "foo/bar" and "foo_baz/bar"
    val absolutePathInStorage = bagLocation.bagPathInStorage
      .replaceAll("/*$", "") + "/"

    s3Client.listObjects(
      bagLocation.storageNamespace,
      bagLocation.bagPathInStorage
    )
  }

  private def listBagItems(bagLocation: BagLocation)(
    implicit s3Client: AmazonS3) = {
    // TODO: limit size of the returned List and use Marker to paginate(?), but needs care if bag contents can change during copy.
    debug(s"listing items in $bagLocation")

    val objectListing = listObjects(s3Client, bagLocation)

    objectListing.getObjectSummaries.asScala
      .map(summary => {
        val itemPathInBag =
          summary.getKey.stripPrefix(s"${bagLocation.bagPathInStorage}/")
        BagItemLocation(bagLocation, itemPathInBag)
      })
      .toList
  }

  private def duplicateBagItems(sourceBagItems: List[BagItemLocation],
                                storageDestination: StorageLocation)(
    implicit s3Copier: S3Copier,
    executionContext: ExecutionContext) = {
    debug(s"duplicating bag items: $sourceBagItems")
    Future.sequence(
      sourceBagItems.map { sourceBagItem =>
        duplicateBagItem(sourceBagItem, storageDestination)
      }
    )
  }

  private def duplicateBagItem(
    sourceBagItem: BagItemLocation,
    storageDestination: StorageLocation)(implicit s3Copier: S3Copier) = {
    val sourceNamespace = sourceBagItem.bagLocation.storageNamespace
    val sourceItemKey =
      s"${sourceBagItem.bagLocation.bagPathInStorage}/${sourceBagItem.itemPath}"
    val destinationNamespace = storageDestination.namespace
    val destinationItemKey =
      s"${storageDestination.rootPath}/${sourceBagItem.bagLocation.bagPath}/${sourceBagItem.itemPath}"

    debug(
      s"duplicating bag item $sourceNamespace/$sourceItemKey -> $destinationNamespace/$destinationItemKey")
    s3Copier.copy(
      sourceNamespace,
      sourceItemKey,
      destinationNamespace,
      destinationItemKey)
  }
}
