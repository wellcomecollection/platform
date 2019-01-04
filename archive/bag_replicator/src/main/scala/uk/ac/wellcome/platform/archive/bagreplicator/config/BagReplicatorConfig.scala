package uk.ac.wellcome.platform.archive.bagreplicator.config

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.platform.archive.bagreplicator.models.StorageLocation

case class BagReplicatorConfig(parallelism: Int, destination: StorageLocation)

object BagReplicatorConfig {
  def buildBagUploaderConfig(config: Config) = {
    BagReplicatorConfig(
      parallelism =
        config.getOrElse[Int]("bag-replicator.parallelism")(default = 10),
      destination = StorageLocation(
        namespace =
          config.required[String]("bag-replicator.storage.destination.bucket"),
        rootPath =
          config.required[String]("bag-replicator.storage.destination.rootpath")
      )
    )
  }
}
