package uk.ac.wellcome.platform.archive.bagreplicator.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectListing, S3ObjectSummary}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.bagreplicator.config.ReplicatorDestinationConfig
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagItemLocation,
  BagItemPath,
  BagLocation
}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class BagStorage(s3Client: AmazonS3)(implicit ec: ExecutionContext)
    extends Logging {

  val s3Copier = new S3Copier(s3Client)

  def duplicateBag(
    sourceBagLocation: BagLocation,
    storageDestination: ReplicatorDestinationConfig
  ): Future[BagLocation] = {
    debug(s"duplicating bag from $sourceBagLocation to $storageDestination")

    val dstBagLocation = sourceBagLocation.copy(
      storageNamespace = storageDestination.namespace,
      storagePrefix = storageDestination.rootPath
    )

    for {
      sourceBagItems <- listBagItems(sourceBagLocation)
      _ <- duplicateBagItems(
        sourceBagItems = sourceBagItems,
        dstBagLocation = dstBagLocation
      )
    } yield dstBagLocation
  }

  private def listObjects(bagLocation: BagLocation): Future[ObjectListing] =
    Future {
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
    dstBagLocation: BagLocation
  ): Future[List[BagItemLocation]] = {
    debug(s"duplicating bag items: $sourceBagItems")

    Future.sequence(
      sourceBagItems.map { srcBagItemLocation =>
        val dstBagItemLocation = srcBagItemLocation.copy(
          bagLocation = dstBagLocation
        )

        val future = s3Copier.copy(
          src = srcBagItemLocation.objectLocation,
          dst = dstBagItemLocation.objectLocation
        )

        future.map { _ =>
          dstBagItemLocation
        }
      }
    )
  }
}
