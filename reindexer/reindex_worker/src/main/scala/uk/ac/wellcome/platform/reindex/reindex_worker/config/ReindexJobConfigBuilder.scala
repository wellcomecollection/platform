package uk.ac.wellcome.platform.reindex.reindex_worker.config

import com.typesafe.config.Config
import uk.ac.wellcome.config.core.builders.EnrichConfig._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJobConfig

object ReindexJobConfigBuilder {
  def buildReindexJobConfigMap(config: Config): Map[String, ReindexJobConfig] = {
    val jsonString = config.required("reindexer.jobConfig")
    fromJson[Map[String, ReindexJobConfig]](jsonString).getOrElse(
      throw new RuntimeException(s"Unable to parse reindexer.jobConfig: <<$jsonString>>")
    )
  }
}
