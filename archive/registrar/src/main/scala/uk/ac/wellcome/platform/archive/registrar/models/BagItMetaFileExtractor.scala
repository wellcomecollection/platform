package uk.ac.wellcome.platform.archive.registrar.models

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.util.IOUtils
import uk.ac.wellcome.platform.archive.common.models.BagLocation
import uk.ac.wellcome.storage.ObjectLocation

object BagItMetaFileExtractor {
  private def extractMap(s3Client: AmazonS3)(
    location: ObjectLocation, delimiter: String
  ): Map[String, String] = {
    val obj = s3Client.getObject(
      location.namespace,
      location.key
    )

    val lines = IOUtils
      .toString(obj.getObjectContent)
      .split("\n")
      .toList

    lines
      .map(_.split(delimiter, 2))
      .map(splitLine => splitLine(0) -> splitLine(1))
      .toMap
  }

  private def createBagItMetaFileLocation(
                                           bagLocation: BagLocation,
                                           name: String
                                         ): ObjectLocation =
    ObjectLocation(
      bagLocation.storageNamespace,
      List(
        bagLocation.storagePath,
        bagLocation.bagPath.value,
        name
      ).mkString("/")
    )

  def get(s3Client: AmazonS3, bagLocation: BagLocation)(
    config: BagMetaFileConfig
  ) = {
    val location = createBagItMetaFileLocation(bagLocation, config.name)
    extractMap(s3Client)(location, config.delimiter)
  }
}

case class BagMetaFileConfig(name: String, delimiter: String)