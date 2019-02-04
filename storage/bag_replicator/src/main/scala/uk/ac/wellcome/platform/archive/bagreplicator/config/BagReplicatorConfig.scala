package uk.ac.wellcome.platform.archive.bagreplicator.config

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import ReplicatorDestinationConfig

case class BagReplicatorConfig(parallelism: Int, destination: ReplicatorDestinationConfig)

object BagReplicatorConfig {
  def buildBagUploaderConfig(config: Config) = {
    BagReplicatorConfig(
      parallelism =
        config.getOrElse[Int]("bag-replicator.parallelism")(default = 10),
      destination = ReplicatorDestinationConfig(
        namespace =
          config.required[String]("bag-replicator.storage.destination.bucket"),
        rootPath =
          config.required[String]("bag-replicator.storage.destination.rootpath")
      )
    )
  }
}
