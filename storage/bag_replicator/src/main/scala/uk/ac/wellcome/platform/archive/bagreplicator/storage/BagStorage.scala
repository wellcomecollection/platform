package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectListing, S3ObjectSummary}
import com.amazonaws.services.s3.transfer.model.CopyResult

import grizzled.slf4j.Logging

import uk.ac.wellcome.platform.archive.bagreplicator.models.StorageLocation
import uk.ac.wellcome.platform.archive.common.models.BagLocation

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}


object BagStorage extends Logging {

  def duplicateBag(
    sourceBagLocation: BagLocation,
    storageDestination: StorageLocation
  )(implicit
      s3Client: AmazonS3,
      s3Copier: S3Copier,
    ctx: ExecutionContext
  ): Future[List[CopyResult]] = {
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
    ctx: ExecutionContext
  ): Future[ObjectListing] = Future {

    val absolutePathInStorage = bagLocation
      .bagPathInStorage
      .replaceAll("(.*[^/]+)/*", "$1/")

    s3Client.listObjects(
      bagLocation.storageNamespace,
      absolutePathInStorage
    )
  }

  private def getItemInPath(
    summary: S3ObjectSummary,
    prefix: String
  ) = summary
        .getKey
        .stripPrefix(prefix)

  private def getObjectSummaries(
    listing: ObjectListing,
    bagLocation: BagLocation
  )(implicit
    ctx: ExecutionContext
  ): Future[List[BagItemLocation]] = Future {
    val prefix = s"${bagLocation.bagPathInStorage}/"

    listing
      .getObjectSummaries.asScala
      .map(getItemInPath(_,prefix))
      .map(BagItemLocation(bagLocation,_))
      .toList
  }

  private def listBagItems(
    location: BagLocation
  )(implicit
      s3Client: AmazonS3,
      ctx: ExecutionContext
  ): Future[List[BagItemLocation]] = {
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
    storageDestination: StorageLocation
  )(implicit
      s3Copier: S3Copier,
      ctx: ExecutionContext
  ): Future[List[CopyResult]] = {
    debug(s"duplicating bag items: $sourceBagItems")

    Future.sequence(
      sourceBagItems.map { sourceBagItem =>
        duplicateBagItem(sourceBagItem, storageDestination)
      }
    )
  }

  private def duplicateBagItem(
    sourceBagItem: BagItemLocation,
    storageDestination: StorageLocation
  )(implicit
      s3Copier: S3Copier,
      ctx: ExecutionContext
  ): Future[CopyResult] = {

    val sourceNamespace = sourceBagItem.bagLocation.storageNamespace
    val sourceItemKey = List(
      sourceBagItem.bagLocation.bagPathInStorage,
      sourceBagItem.itemPath
    ).mkString("/")

    val destinationNamespace = storageDestination.namespace
    val destinationItemKey = List(
      storageDestination.rootPath,
      sourceBagItem.bagLocation.bagPath,
      sourceBagItem.itemPath
    ).mkString("/")

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
