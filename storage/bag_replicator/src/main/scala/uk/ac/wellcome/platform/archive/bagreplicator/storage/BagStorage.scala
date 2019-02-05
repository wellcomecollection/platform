package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectListing, S3ObjectSummary}
import com.amazonaws.services.s3.transfer.model.CopyResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemLocation,
  BagItemPath,
  BagLocation
}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class BagStorage(s3Client: AmazonS3)(implicit ec: ExecutionContext) extends Logging {

  val s3Copier = new S3Copier(s3Client)

  def duplicateBag(
    sourceBagLocation: BagLocation,
    storageDestination: ReplicatorDestinationConfig
  ): Future[List[CopyResult]] = {
    debug(s"duplicating bag from $sourceBagLocation to $storageDestination")

    for {
      sourceBagItems <- listBagItems(sourceBagLocation)
      copyResults <- duplicateBagItems(
        sourceBagItems,
        storageDestination
      )
    } yield copyResults
  }

  private def listObjects(bagLocation: BagLocation): Future[ObjectListing] = Future {
    val absolutePathInStorage = bagLocation.completePath
      .replaceAll("(.*[^/]+)/*", "$1/")

    s3Client.listObjects(
      bagLocation.storageNamespace,
      absolutePathInStorage
    )
  }

  private def getItemInPath(
    summary: S3ObjectSummary,
    prefix: String
  ): String =
    summary.getKey
      .stripPrefix(prefix)

  private def getObjectSummaries(
    listing: ObjectListing,
    bagLocation: BagLocation
  ): Future[List[BagItemLocation]] = Future {
    val prefix = s"${bagLocation.completePath}/"

    listing.getObjectSummaries.asScala
      .map(getItemInPath(_, prefix))
      .map { path: String =>
        BagItemLocation(
          bagLocation = bagLocation,
          bagItemPath = BagItemPath(path)
        )
      }
      .toList
  }

  private def listBagItems(
    location: BagLocation
  ): Future[List[BagItemLocation]] = {
    // TODO: limit size of the returned List
    // use Marker to paginate(?)
    // needs care if bag contents can change during copy.
    debug(s"listing items in $location")

    for {
      listing <- listObjects(location)
      summaries <- getObjectSummaries(listing, location)
    } yield summaries
  }

  private def duplicateBagItems(
    sourceBagItems: List[BagItemLocation],
    storageDestination: ReplicatorDestinationConfig
  ): Future[List[CopyResult]] = {
    debug(s"duplicating bag items: $sourceBagItems")

    Future.sequence(
      sourceBagItems.map { sourceBagItem =>
        duplicateBagItem(sourceBagItem, storageDestination)
      }
    )
  }

  private def duplicateBagItem(
    sourceBagItemLocation: BagItemLocation,
    storageDestination: ReplicatorDestinationConfig
  ): Future[CopyResult] = {

    val sourceNamespace = sourceBagItemLocation.bagLocation.storageNamespace
    val sourceItemKey = sourceBagItemLocation.completePath

    val destinationBagLocation = sourceBagItemLocation.bagLocation.copy(
      storagePrefix = storageDestination.rootPath
    )
    val destinationBagItem = sourceBagItemLocation.copy(
      bagLocation = destinationBagLocation
    )

    val destinationNamespace = storageDestination.namespace
    val destinationItemKey = destinationBagItem.completePath

    debug(
      List(
        "duplicating bag item",
        s"$sourceNamespace/$sourceItemKey",
        s"-> $destinationNamespace/$destinationItemKey"
      ).mkString(" ")
    )

    s3Copier.copy(
      sourceNamespace,
      sourceItemKey,
      destinationNamespace,
      destinationItemKey
    )
  }
}
