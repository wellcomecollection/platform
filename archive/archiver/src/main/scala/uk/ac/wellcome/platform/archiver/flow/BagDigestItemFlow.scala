package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object BagDigestItemFlow extends Logging {
  def apply(config: BagUploaderConfig)
    : Flow[(ObjectLocation, BagName, ZipFile), BagDigestItem, NotUsed] = {

    val fileSplitterFlow = FileSplitterFlow(config)

    Flow[(ObjectLocation, BagName, ZipFile)]
      .log("digest location")
      .flatMapConcat {
        case (objectLocation, bagName, zipFile) =>
          Source
            .single((objectLocation, zipFile))
            .via(fileSplitterFlow)
            .map(stringArray => (stringArray, bagName))
            .map {
              case (Array(checksum: String, key: String), bag) =>
                BagDigestItem(checksum, ObjectLocation(bag.value, key))
              case (default, bag) =>
                throw MalformedBagDigestException(
                  default.mkString(config.digestDelimiter),
                  bag)
            }
      }
      .log("bag digest item")
  }
}

case class BagDigestItem(checksum: String, location: ObjectLocation)

case class MalformedBagDigestException(line: String, bagName: BagName)
    extends RuntimeException(
      s"Malformed bag digest line: $line in ${bagName.value}")
