package uk.ac.wellcome.models.transformable.miro

import java.io.InputStream
import scala.io.Source

import uk.ac.wellcome.utils.JsonUtil

object MiroContributorMapper {
  val stream: InputStream = getClass
    .getResourceAsStream("/miro_contributor_map.json")
  val jsonString = Source.fromInputStream(stream)

  val contributorMap = JsonUtil.toMap[String](jsonString.mkString).get
}
