package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectListing, S3ObjectSummary}
import com.amazonaws.services.s3.transfer.model.CopyResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.common.models.bagit.{BagLocation, BagItemLocation, BagItemPath}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object BagStorage extends Logging {

  def duplicateBag(
                    sourceBagLocation: BagLocation,
                    storageDestination: ReplicatorDestinationConfig
  )(implicit
    s3Client: AmazonS3,
    s3Copier: S3Copier,
    ctx: ExecutionContext): Future[List[CopyResult]] = {
    debug(
      List(
        s"duplicating bag from",
        sourceBagLocation.toString,
        s"to $storageDestination"
      ).mkString(" ")
    )

    for {
      sourceBagItems <- listBagItems(
        sourceBagLocation
      )

      copyResults <- duplicateBagItems(
        sourceBagItems,
        storageDestination
      )
    } yield copyResults
  }

  private def listObjects(
    s3Client: AmazonS3,
    bagLocation: BagLocation
  )(implicit
    ctx: ExecutionContext): Future[ObjectListing] = Future {

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
  ) =
    summary.getKey
      .stripPrefix(prefix)

  private def getObjectSummaries(
    listing: ObjectListing,
    bagLocation: BagLocation
  )(implicit
    ctx: ExecutionContext): Future[List[BagItemLocation]] = Future {
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
  )(implicit
    s3Client: AmazonS3,
    ctx: ExecutionContext): Future[List[BagItemLocation]] = {
    // TODO: limit size of the returned List
    // use Marker to paginate(?)
    // needs care if bag contents can change during copy.
    debug(s"listing items in $location")

    for {
      listing <- listObjects(s3Client, location)
      summaries <- getObjectSummaries(listing, location)
    } yield summaries
  }

  private def duplicateBagItems(
                                 sourceBagItems: List[BagItemLocation],
                                 storageDestination: ReplicatorDestinationConfig
  )(implicit
    s3Copier: S3Copier,
    ctx: ExecutionContext): Future[List[CopyResult]] = {
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
  )(implicit
    s3Copier: S3Copier,
    ctx: ExecutionContext): Future[CopyResult] = {

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
