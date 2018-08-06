package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation


object DigestLocationFlow {
  def apply(config: BagUploaderConfig): Flow[ZipFile, ObjectLocation, NotUsed] = {
    val bagNameFlow: Flow[ZipFile, BagName, NotUsed] = BagNameFlow()

    bagNameFlow
      .mapConcat(bagName => {
        config.digestNames.map(digestName => {
          ObjectLocation(bagName.value, digestName)
        })
      })
  }
}